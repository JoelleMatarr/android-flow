package com.example.flow22

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
//import androidx.wear.compose.material3.tokens.ColorTokens
import com.checkout.components.core.CheckoutComponentsFactory
import com.checkout.components.interfaces.Environment
import com.checkout.components.interfaces.api.CheckoutComponents
import com.checkout.components.interfaces.component.CheckoutComponentConfiguration
import com.checkout.components.interfaces.component.ComponentCallback
import com.checkout.components.interfaces.component.ComponentOption
import com.checkout.components.interfaces.error.CheckoutError
import com.checkout.components.interfaces.model.PaymentMethodName
import com.checkout.components.interfaces.model.PaymentSessionResponse
import com.checkout.components.interfaces.component.RememberMeConfiguration
import com.checkout.components.interfaces.model.Phone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var checkoutComponents: CheckoutComponents

    private var isCardAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        CHANGE THE PS ID AND PS SECRET MANUALLY
        val paymentSessionID = "ps_34n6r087EaiQeoPG9j6QnlmTBwW"
        val paymentSessionSecret = "pss_d8d58285-b832-49a4-bdfd-ab94e8af938a"

        initializeCheckoutSDK(
            paymentSessionID = paymentSessionID,
            paymentSessionSecret = paymentSessionSecret
        )
    }

    private val customComponentCallback = ComponentCallback(
        onReady = { component ->
            Log.d("flow component", "onReady ${component.name}")
        },
        onSubmit = { component ->
            Log.d("flow component", "onSubmit ${component.name}")
        },
        onSuccess = { component, paymentID ->
            Log.d("flow component success", "${component.name}: $paymentID")
        },
        onError = { component, checkoutError ->
            Log.e("flow component error", "${checkoutError.message}, ${checkoutError.code}")
        },
    )

    private fun handleActivityResult(resultCode: Int, data: String) {
        checkoutComponents.handleActivityResult(resultCode, data)
    }

    private fun initializeCheckoutSDK(
        paymentSessionID: String,
        paymentSessionSecret: String,
    ) {
        //Remember me configuration --> With or Without it, I can see save card checkbox
//        Once RM OTP is submitted it shows me the card form again instead of saved cards
        val rememberMeConfiguration = RememberMeConfiguration(
            data = RememberMeConfiguration.Data(
                email = "jheng-hao.lin8@checkout.com",
//                phone = Phone(
//                    countryCode = "+971",
//                    number = "524123793"
//                )
            ), showPayButton = true
        )

        val componentOption = ComponentOption(
            showPayButton = true, rememberMeConfiguration = rememberMeConfiguration
        )

        val configuration = CheckoutComponentConfiguration(
            context = this,
            paymentSession = PaymentSessionResponse(
                id = paymentSessionID,
                secret = paymentSessionSecret,
            ),
            componentCallback = customComponentCallback,
            publicKey = "pk_sbox_zxmkbjyj4ec7liyyup23gjfsga#",
            environment = Environment.SANDBOX
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkoutComponents = CheckoutComponentsFactory(config = configuration).create()

//With or without the componentOption I can see the save card checkbox
                var flow = checkoutComponents.create(
                    PaymentMethodName.Card,
                    componentOption
                )
                isCardAvailable = flow.isAvailable()

                withContext(Dispatchers.Main) {
                    setContent {
                        var selectedMethod by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row {
                                Button(
                                    onClick = { selectedMethod = "card" },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pay with Card")
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            when (selectedMethod) {
                                "card" -> {
                                    if (isCardAvailable) {
                                        AndroidView(
                                            factory = { context ->
                                                FrameLayout(context).apply {
                                                    addView(flow.provideView(this))
                                                }
                                            }
                                        )
                                    } else {
                                        Text("Card payment is not available.")
                                    }
                                }

                            }
                        }
                    }
                }
            } catch (checkoutError: CheckoutError) {
                Log.e(
                    "flow component error",
                    "Initialization error: ${checkoutError.message}, Details: ${checkoutError.details}"
                )
            }
        }
    }
}

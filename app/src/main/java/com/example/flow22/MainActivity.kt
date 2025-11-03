package com.example.flow22

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkout.components.core.CheckoutComponentsFactory
import com.checkout.components.interfaces.Environment
import com.checkout.components.interfaces.api.CheckoutComponents
import com.checkout.components.interfaces.api.PaymentMethodComponent
import com.checkout.components.interfaces.component.CheckoutComponentConfiguration
import com.checkout.components.interfaces.component.ComponentCallback
import com.checkout.components.interfaces.component.ComponentOption
import com.checkout.components.interfaces.component.RememberMeConfiguration
import com.checkout.components.interfaces.error.CheckoutError
import com.checkout.components.interfaces.model.PaymentMethodName
import com.checkout.components.interfaces.model.PaymentSessionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var checkoutComponents: CheckoutComponents


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        renderContent()
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


    private fun createConfigs(
        paymentSessionID: String,
        paymentSessionSecret: String,
    ): Pair<ComponentOption, CheckoutComponentConfiguration> {
        val rememberMeConfiguration = RememberMeConfiguration(
            data = RememberMeConfiguration.Data(
                email = "jheng-hao.lin8@checkout.com",
            ), showPayButton = true
        )

        val componentOption = ComponentOption(
            showPayButton = true, rememberMeConfiguration = rememberMeConfiguration
        )

        val config = CheckoutComponentConfiguration(
            context = this,
            paymentSession = PaymentSessionResponse(
                id = paymentSessionID,
                secret = paymentSessionSecret,
            ),
            componentCallback = customComponentCallback,
            publicKey = "pk_sbox_zxmkbjyj4ec7liyyup23gjfsga#",
            environment = Environment.SANDBOX
        )

        return componentOption to config
    }

    private fun renderContent() {
        setContent {
            var flow by remember { mutableStateOf<PaymentMethodComponent?>(null) }
            var selectedMethod by remember { mutableStateOf("") }
            var paymentSessionID by remember { mutableStateOf("") }
            var paymentSessionSecret by remember { mutableStateOf("") }
            var isCardAvailable by remember { mutableStateOf(false) }


            LaunchedEffect(selectedMethod) {
                if (selectedMethod == "card") {
                    val (options, config) = createConfigs(paymentSessionID, paymentSessionSecret)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            checkoutComponents = CheckoutComponentsFactory(config = config).create()

                            flow = checkoutComponents.create(
                                PaymentMethodName.Card,
                                options
                            )
                            isCardAvailable = flow?.isAvailable() ?: false

                        } catch (checkoutError: CheckoutError) {
                            Log.e(
                                "flow component error",
                                "Initialization error: ${checkoutError.message}, Details: ${checkoutError.details}"
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = paymentSessionID, label = { Text("Payment Session ID") },
                        onValueChange = { paymentSessionID = it },
                        modifier = Modifier.fillParentMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = paymentSessionSecret, label = { Text("Payment Session Secret") },
                        onValueChange = { paymentSessionSecret = it },
                        modifier = Modifier.fillParentMaxWidth()
                    )
                }

                item {
                    Row {
                        Button(
                            enabled = paymentSessionID.isNotBlank() && paymentSessionSecret.isNotBlank(),
                            onClick = {
                                if (paymentSessionID.isEmpty() || paymentSessionSecret.isEmpty()) {
                                    Log.e("flow component error", "Please enter Payment Session ID and Secret")
                                    return@Button
                                } else {
                                    selectedMethod = "card"
                                }
                            },
                        ) {
                            Text("Render Card Component")
                        }
                    }
                }

                item {
                    when (selectedMethod) {
                        "card" -> {
                            if (isCardAvailable) {
                                flow?.Render()
                            }
                        }
                    }
                }
            }
        }
    }
}

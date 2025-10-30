package com.example.flow22

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.checkout.components.core.CheckoutComponentsFactory
import com.checkout.components.interfaces.Environment
import com.checkout.components.interfaces.api.CheckoutComponents
import com.checkout.components.interfaces.component.CheckoutComponentConfiguration
import com.checkout.components.interfaces.component.ComponentCallback
import com.checkout.components.interfaces.component.ComponentOption
import com.checkout.components.interfaces.error.CheckoutError
import com.checkout.components.interfaces.model.ComponentName
import com.checkout.components.interfaces.model.PaymentMethodName
import com.checkout.components.interfaces.model.PaymentSessionResponse
import com.checkout.components.wallet.wrapper.GooglePayFlowCoordinator
import com.example.flow22.ui.theme.Flow22Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class MainActivityCopy : ComponentActivity() {
    private lateinit var checkoutComponents: CheckoutComponents
    private lateinit var coordinator: GooglePayFlowCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val paymentSessionID = "ps_33sm8UsfxHFlkxwV7HjQbNz9z93"
        val paymentSessionSecret = "pss_81bcdc85-12f4-40a2-ba15-1112cf1f1565"
        val paymentSessionToken = "YmFzZTY0:eyJpZCI6InBzXzMzc204VXNmeEhGbGt4d1Y3SGpRYk56OXo5MyIsImVudGl0eV9pZCI6ImVudF8ybzRlamE0bm5vM3VreXVudm12dzN2dmtjeSIsImV4cGVyaW1lbnRzIjp7fSwicHJvY2Vzc2luZ19jaGFubmVsX2lkIjoicGNfNnFjdDZ1bGVzN2JlN25jdWFtbzZ3ZWtjaXEiLCJhbW91bnQiOjIwMDAwMCwibG9jYWxlIjoiZW4tR0IiLCJjdXJyZW5jeSI6IlNBUiIsInBheW1lbnRfbWV0aG9kcyI6W3sidHlwZSI6InJlbWVtYmVyX21lIiwiY2FyZF9zY2hlbWVzIjpbIlZpc2EiLCJNYXN0ZXJjYXJkIiwiQW1leCJdLCJlbWFpbCI6IjIzMjMyQGhvdG1haWwuY29tIiwicGhvbmUiOnsibnVtYmVyIjoiNTAwMDAwMDAxIiwiY291bnRyeV9jb2RlIjoiOTcxIn0sImJpbGxpbmdfYWRkcmVzcyI6eyJhZGRyZXNzX2xpbmUxIjoiMTIzIEhpZ2ggU3QuIiwiYWRkcmVzc19saW5lMiI6IkZsYXQgNDU2IiwiY2l0eSI6IkR1YmFpIiwiemlwIjoiU1cxQSAxQUEiLCJjb3VudHJ5IjoiU0EifSwiZGlzcGxheV9tb2RlIjoiY2hlY2tib3gifSx7InR5cGUiOiJjYXJkIiwiY2FyZF9zY2hlbWVzIjpbIlZpc2EiLCJNYXN0ZXJjYXJkIiwiQW1leCJdLCJzY2hlbWVfY2hvaWNlX2VuYWJsZWQiOmZhbHNlLCJzdG9yZV9wYXltZW50X2RldGFpbHMiOiJlbmFibGVkIiwiYmlsbGluZ19hZGRyZXNzIjp7ImFkZHJlc3NfbGluZTEiOiIxMjMgSGlnaCBTdC4iLCJhZGRyZXNzX2xpbmUyIjoiRmxhdCA0NTYiLCJjaXR5IjoiRHViYWkiLCJ6aXAiOiJTVzFBIDFBQSIsImNvdW50cnkiOiJTQSJ9fSx7InR5cGUiOiJhcHBsZXBheSIsImRpc3BsYXlfbmFtZSI6IkpvZWxsZU1UZXN0IC0gcm0iLCJjb3VudHJ5X2NvZGUiOiJHQiIsImN1cnJlbmN5X2NvZGUiOiJTQVIiLCJtZXJjaGFudF9jYXBhYmlsaXRpZXMiOlsic3VwcG9ydHMzRFMiXSwic3VwcG9ydGVkX25ldHdvcmtzIjpbInZpc2EiLCJtYXN0ZXJDYXJkIiwiYW1leCJdLCJ0b3RhbCI6eyJsYWJlbCI6IkpvZWxsZU1UZXN0IC0gcm0iLCJ0eXBlIjoiZmluYWwiLCJhbW91bnQiOiIyMDAwIn19LHsidHlwZSI6Imdvb2dsZXBheSIsIm1lcmNoYW50Ijp7ImlkIjoiMDgxMTMwODkzODYyNjg4NDk5ODIiLCJuYW1lIjoiSm9lbGxlTVRlc3QgLSBybSIsIm9yaWdpbiI6Imh0dHBzOi8vZ29vZ2xlLmNvbSJ9LCJ0cmFuc2FjdGlvbl9pbmZvIjp7InRvdGFsX3ByaWNlX3N0YXR1cyI6IkZJTkFMIiwidG90YWxfcHJpY2UiOiIyMDAwIiwiY291bnRyeV9jb2RlIjoiR0IiLCJjdXJyZW5jeV9jb2RlIjoiU0FSIn0sImNhcmRfcGFyYW1ldGVycyI6eyJhbGxvd2VkX2F1dGhfbWV0aG9kcyI6WyJQQU5fT05MWSIsIkNSWVBUT0dSQU1fM0RTIl0sImFsbG93ZWRfY2FyZF9uZXR3b3JrcyI6WyJWSVNBIiwiTUFTVEVSQ0FSRCIsIkFNRVgiXX19LHsidHlwZSI6InRhYmJ5IiwiY291bnRyeV9jYWxsaW5nX2NvZGVzIjpbIjk2NiJdLCJlbWFpbCI6IjIzMjMyQGhvdG1haWwuY29tIiwibmFtZSI6IkppYSBUc2FuZyJ9XSwiZmVhdHVyZV9mbGFncyI6WyJhbmFseXRpY3Nfb2JzZXJ2YWJpbGl0eV9lbmFibGVkIiwiY2FyZF9maWVsZHNfZW5hYmxlZCIsImdldF93aXRoX3B1YmxpY19rZXlfZW5hYmxlZCIsImxvZ3Nfb2JzZXJ2YWJpbGl0eV9lbmFibGVkIiwicmlza19qc19lbmFibGVkIiwidXNlX25vbl9iaWNfaWRlYWxfaW50ZWdyYXRpb24iXSwicmlzayI6eyJlbmFibGVkIjpmYWxzZX0sIm1lcmNoYW50X25hbWUiOiJKb2VsbGVNVGVzdCAtIHJtIiwicGF5bWVudF9zZXNzaW9uX3NlY3JldCI6InBzc184MWJjZGM4NS0xMmY0LTQwYTItYmExNS0xMTEyY2YxZjE1NjUiLCJpbnRlZ3JhdGlvbl9kb21haW4iOiJkZXZpY2VzLmFwaS5zYW5kYm94LmNoZWNrb3V0LmNvbSJ9";
        coordinator = GooglePayFlowCoordinator(
            context = this, // Now `this` is safe
            handleActivityResult = { resultCode, data ->
                handleActivityResult(resultCode, data)
            }
        )

        val flowCoordinators = mapOf(PaymentMethodName.GooglePay to coordinator)



        initializeCheckoutSDK(
            paymentSessionID = paymentSessionID,
            paymentSessionToken = paymentSessionToken,
            paymentSessionSecret = paymentSessionSecret,
            flowCoordinators = flowCoordinators
        )
    }
    val customComponentCallback = ComponentCallback(
        onReady = { component ->
            Log.d("flow component", "test onReady " + component.name)
        },
        onSubmit = { component ->
            Log.d("flow component ", "test onSubmit " + component.name)
        },
        onSuccess = { component, paymentID ->
            Log.d("flow component payment success ${component.name}", paymentID)
        },
        onError = { component, checkoutError ->
            Log.d(
                "flow component callback Error",
                "onError " + checkoutError.message + ", " + checkoutError.code
            )
        },
    )

    private fun handleActivityResult(resultCode: Int, data: String) {
        checkoutComponents.handleActivityResult(resultCode, data)
    }

    private fun initializeCheckoutSDK(
        paymentSessionID: String,
        paymentSessionToken: String,
        paymentSessionSecret: String,
        flowCoordinators: Map<PaymentMethodName, GooglePayFlowCoordinator>,
    ) {
        val configuration = CheckoutComponentConfiguration(
            context = this,
            paymentSession = PaymentSessionResponse(
                id = paymentSessionID,
                paymentSessionToken = paymentSessionToken,
                paymentSessionSecret = paymentSessionSecret,
            ),
            componentCallback = customComponentCallback,
            publicKey = "pk_sbox_cwlkrqiyfrfceqz2ggxodhda2yh",
            environment = Environment.SANDBOX,
            flowCoordinators = flowCoordinators
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkoutComponents = CheckoutComponentsFactory(config = configuration).create()

                val flow = checkoutComponents.create(PaymentMethodName.Card, ComponentOption(showPayButton = false))
                val googlePay = checkoutComponents.create(PaymentMethodName.GooglePay)
    print(googlePay)
                withContext(Dispatchers.Main) {
                    setContent {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Render Flow Component
                            AndroidView(
                                factory = { context ->
                                    val flowContainer = FrameLayout(context)
                                    flowContainer.addView(flow.provideView(flowContainer))
                                    flowContainer
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Render Google Pay Component
                            AndroidView(
                                factory = { context ->
                                    val googlePayContainer = FrameLayout(context)
                                    googlePayContainer.addView(googlePay.provideView(googlePayContainer))
                                    googlePayContainer
                                }
                            )
                        }
                    }
                }
            } catch (checkoutError: CheckoutError) {
                Log.d(
                    "flow component error on init",
                    "Error initializing Checkout SDK: ${checkoutError.message}, Details: ${checkoutError.details}"
                )
            }
        }
    }
}

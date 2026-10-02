package com.example.flow22

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.lifecycle.lifecycleScope
import com.checkout.components.core.CheckoutComponentsFactory
import com.checkout.components.interfaces.Environment
import com.checkout.components.interfaces.api.CheckoutComponents
import com.checkout.components.interfaces.api.PaymentMethodComponent
import com.checkout.components.interfaces.component.CardConfiguration
import com.checkout.components.interfaces.component.CheckoutComponentConfiguration
import com.checkout.components.interfaces.component.ComponentCallback
import com.checkout.components.interfaces.component.ComponentOption
import com.checkout.components.interfaces.component.RememberMeConfiguration
import com.checkout.components.interfaces.component.StoredCardConfiguration
import com.checkout.components.interfaces.error.CheckoutError
import com.checkout.components.interfaces.localisation.Locale
import com.checkout.components.interfaces.model.CardholderNamePosition
import com.checkout.components.interfaces.model.ComponentName
import com.checkout.components.interfaces.model.PaymentMethodName
import com.checkout.components.interfaces.model.PaymentSessionResponse
import com.checkout.components.interfaces.model.Phone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var checkoutComponents: CheckoutComponents

    // Drives the custom pay button. Only ever written from a deferred coroutine,
    // never from inside a component callback (see note on onReady/onChange below).
    private var isComponentValid by mutableStateOf(false)
    private var isSubmitting by mutableStateOf(false)
    private var statusMessage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        renderContent()
    }

    /**
     * The SDK invokes these callbacks from inside its own `@Composable Render()` pass.
     * Touching the component there (`component.name`, `component.isValid()`) reads Compose
     * state the SDK created during that same composition, which throws
     * IllegalStateException: "Reading a state that was created after the snapshot was taken".
     * So every callback body just posts work onto the main looper and reads the component
     * after the frame has been applied.
     */
    private val customComponentCallback = ComponentCallback(
        onReady = { component ->
            lifecycleScope.launch {
                runCatching {
                    isComponentValid = component.isValid()
                    Log.d("flow component", "onReady ${component.name} isValid=$isComponentValid")
                }.onFailure {
                    isComponentValid = false
                    Log.e("flow component error", "onReady check failed: ${it.message}", it)
                }
            }
        },
        onChange = { component ->
            lifecycleScope.launch {
                runCatching {
                    isComponentValid = component.isValid()
                }.onFailure {
                    isComponentValid = false
                    Log.e("flow component error", "onChange check failed: ${it.message}", it)
                }
            }
        },
        onSubmit = { component ->
            lifecycleScope.launch {
                isSubmitting = true
                statusMessage = "Submitting…"
                runCatching { Log.d("flow component", "onSubmit ${component.name}") }
            }
        },
        onSuccess = { component, paymentID ->
            lifecycleScope.launch {
                isSubmitting = false
                statusMessage = "Success: $paymentID"
                runCatching { Log.d("flow component success", "${component.name}: $paymentID") }
            }
        },
        onError = { _, checkoutError ->
            lifecycleScope.launch {
                isSubmitting = false
                statusMessage = "Error ${checkoutError.code}: ${checkoutError.message}"
                Log.e(
                    "flow component error",
                    "code=${checkoutError.code} message=${checkoutError.message} " +
                        "details=${checkoutError.details}",
                )
            }
        },
    )

    private fun createConfigs(
        paymentSessionID: String,
        paymentSessionSecret: String,
    ): Pair<ComponentOption, CheckoutComponentConfiguration> {
        val cardConfiguration = CardConfiguration(
            displayCardholderName = CardholderNamePosition.HIDDEN
        )

        // This screen renders its own "Pay Now" button, so the SDK must not render one too.
        // Two buttons both calling submit() on a single-use payment session makes the second
        // attempt fail.
        val componentOption = ComponentOption(
            showPayButton = false,
            cardConfiguration = cardConfiguration
        )

        val storedCardOptions = ComponentOption(
            storedCardConfiguration = StoredCardConfiguration(captureCardCvv = true),
            rememberMeConfiguration = RememberMeConfiguration(
                data = RememberMeConfiguration.Data(
                    email = "joelle.matar@checkout.com",
                    phone = Phone(countryCode = "44", number = "7700900000"),
                ),
            ),
        )
        val config = CheckoutComponentConfiguration(
            context = this,
            paymentSession = PaymentSessionResponse(
                id = paymentSessionID,
                secret = paymentSessionSecret,
            ),
            componentCallback = customComponentCallback,
            publicKey = "pk_sbox_zxmkbjyj4ec7liyyup23gjfsga#",
            environment = Environment.SANDBOX,
            locale = Locale.En,
            appearance = CheckoutAppearance(this).createDesignTokens(),
            componentOptions = mapOf(PaymentMethodName.StoredCard to storedCardOptions),
        )

        return componentOption to config
    }

    private fun renderContent() {
        setContent {
            var flow by remember { mutableStateOf<PaymentMethodComponent?>(null) }
            var paymentSessionID by remember { mutableStateOf("") }
            var paymentSessionSecret by remember { mutableStateOf("") }
            var isCardAvailable by remember { mutableStateOf(false) }
            // Incremented on every tap so a fresh payment session re-initializes the SDK.
            var initAttempt by remember { mutableStateOf(0) }

            LaunchedEffect(initAttempt) {
                if (initAttempt == 0) return@LaunchedEffect

                isCardAvailable = false
                isComponentValid = false
                flow = null
                statusMessage = "Initializing…"

                val (options, config) = createConfigs(
                    paymentSessionID.trim(),
                    paymentSessionSecret.trim(),
                )

                try {
                    val component = withContext(Dispatchers.IO) {
                        checkoutComponents = CheckoutComponentsFactory(config = config).create()
//                        checkoutComponents.create(PaymentMethodName.Card, options)
                        checkoutComponents.create(ComponentName.Flow, options)
                    }
                    val available = withContext(Dispatchers.IO) { component.isAvailable() }

                    flow = component
                    isCardAvailable = available
                    statusMessage = if (available) {
                        "Card component ready"
                    } else {
                        "Card is not available for this payment session"
                    }
                } catch (checkoutError: CheckoutError) {
                    statusMessage = "Init error ${checkoutError.code}: ${checkoutError.message}"
                    Log.e(
                        "flow component error",
                        "Initialization error: code=${checkoutError.code} " +
                            "message=${checkoutError.message} details=${checkoutError.details}",
                    )
                } catch (throwable: Throwable) {
                    // Anything the SDK throws that is not a CheckoutError used to kill the
                    // process from the detached coroutine this ran in.
                    statusMessage = "Init failed: ${throwable.message}"
                    Log.e("flow component error", "Initialization failed", throwable)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(40.dp),
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
                            enabled = paymentSessionID.isNotBlank() &&
                                paymentSessionSecret.isNotBlank(),
                            onClick = { initAttempt++ },
                        ) {
                            Text("Render Card Component")
                        }
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    item { Text(statusMessage) }
                }

                item {
                    if (isCardAvailable) {
                        flow?.Render()
                    }
                }

                if (isCardAvailable) {
                    item {
                        Button(
                            enabled = isComponentValid && !isSubmitting,
                            onClick = {
                                lifecycleScope.launch {
                                    isSubmitting = true
                                    runCatching { flow?.submit() }.onFailure {
                                        isSubmitting = false
                                        statusMessage = "Submit failed: ${it.message}"
                                        Log.e("flow component error", "submit() threw", it)
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(if (isSubmitting) "Processing…" else "Pay Now")
                        }
                    }
                }
            }
        }
    }
}

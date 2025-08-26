package io.snabble.sdk.ui.checkout

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts
import com.google.android.material.appbar.MaterialToolbar
import io.snabble.sdk.InitializationState
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkout.Checkout
import io.snabble.sdk.checkout.CheckoutState
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.remotetheme.onToolBarColorForProject
import io.snabble.sdk.ui.remotetheme.toolBarColorForProject
import io.snabble.sdk.utils.Logger

class CheckoutActivity : FragmentActivity() {
    companion object {

        const val ARG_PROJECT_ID = "projectId"

        private const val HSV_COLOR_SIZE = 3
        private const val DARKEN_FACTOR = 0.2f

        @JvmStatic
        fun startCheckoutFlow(context: Context, newTask: Boolean = false) {
            val intent = Intent(context, CheckoutActivity::class.java).apply {
                setPackage(context.packageName)
                if (newTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun restoreCheckoutIfNeeded(context: Context) {
            Snabble.initializationState.observeForever(object : Observer<InitializationState> {
                override fun onChanged(value: InitializationState) {
                    if (value == InitializationState.INITIALIZED) {
                        Snabble.initializationState.removeObserver(this)
                        val project = Snabble.checkedInProject.value
                        if (project?.checkout?.state?.value?.isCheckoutState == true) {
                            startCheckoutFlow(context, true)
                        }
                    }
                }
            })
        }
    }

    private lateinit var navGraph: NavGraph
    private lateinit var navController: NavController
    private var checkout: Checkout? = null

    init {
        with(Snabble.checkedInProject.value?.googlePayHelper) {
            this?.paymentDataLauncher =
                this@CheckoutActivity.registerForActivityResult(
                    /* contract = */ TaskResultContracts.GetPaymentDataResult()
                ) { result: ApiTaskResult<PaymentData?>? ->
                    result?.let {
                        onResult(
                            resultCode = it.status,
                            paymentData = it.result
                        )
                    }
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            if (checkout?.state?.value == CheckoutState.PAYMENT_APPROVED) {
                checkout?.reset()
                finish()
            }
        }

        if (Snabble.initializationState.value != InitializationState.INITIALIZED) {
            Snabble.setup(application)
        }

        Snabble.initializationState.observe(this) initObserver@{
            when (it) {
                InitializationState.INITIALIZED -> {
                    Snabble.checkedInProject.observe(this) { project ->
                        setContentView(R.layout.snabble_activity_checkout)

                        val navHostFragment = supportFragmentManager.findFragmentById(
                            R.id.nav_host_container
                        ) as NavHostFragment
                        val graphInflater = navHostFragment.navController.navInflater
                        navGraph = graphInflater.inflate(R.navigation.snabble_nav_checkout)
                        navController = navHostFragment.navController
                        setUpToolBarAndStatusBar()

                        if (project == null) {
                            finishWithError("Project not set")
                            return@observe
                        }

                        val checkout = project.checkout
                        val state = checkout.state.value
                        if (state?.isCheckoutState == false) {
                            finishWithError("Unexpected checkout state ${state.name}")
                            return@observe
                        }
                        this.checkout = checkout

                        checkout.state.observe(this) {
                            onStateChanged()
                        }

                        val startDestinationId = getNavigationId()
                        if (startDestinationId == null) {
                            finish()
                            return@observe
                        } else {
                            navGraph.setStartDestination(startDestinationId)
                        }

                        navController.graph = navGraph
                    }
                }

                InitializationState.UNINITIALIZED,
                InitializationState.INITIALIZING,
                null -> Unit // ignore
                InitializationState.ERROR -> {
                    finishWithError("The snabble SDK is not initialized")
                }
            }
        }
    }

    private fun setUpToolBarAndStatusBar() {
        val showToolBar = resources.getBoolean(R.bool.showToolbarInCheckout)
        findViewById<View>(R.id.checkout_toolbar)?.isVisible = showToolBar

        navController.addOnDestinationChangedListener { _, _, arguments ->
            findViewById<View>(R.id.checkout_toolbar)?.isVisible =
                arguments?.getBoolean("showToolbar", false) == true

            val toolBarColor =
                this@CheckoutActivity.toolBarColorForProject(Snabble.checkedInProject.value)
            if (showToolBar) toolBarColor?.let { applyStatusBarStyling(toolBarColor) }
            val onToolBarColor =
                this@CheckoutActivity.onToolBarColorForProject(Snabble.checkedInProject.value)
            this.findViewById<MaterialToolbar>(R.id.checkout_toolbar).apply {
                toolBarColor?.let(::setBackgroundColor)
                onToolBarColor?.let(::setTitleTextColor)
            }
        }
    }

    private fun finishWithError(error: String) {
        Logger.e(error)
        finish()
    }

    private fun getNavigationId(): Int? {
        val checkout = checkout ?: return null
        return when (checkout.state.value) {
            CheckoutState.WAIT_FOR_GATEKEEPER -> {
                R.id.snabble_nav_routing_gatekeeper
            }

            CheckoutState.WAIT_FOR_SUPERVISOR -> {
                R.id.snabble_nav_routing_supervisor
            }

            CheckoutState.WAIT_FOR_APPROVAL -> {
                val selectedPaymentMethod = checkout.selectedPaymentMethod
                selectedPaymentMethod?.let {
                    when (it) {
                        PaymentMethod.CUSTOMERCARD_POS -> {
                            R.id.snabble_nav_checkout_customercard
                        }

                        PaymentMethod.QRCODE_POS -> {
                            R.id.snabble_nav_checkout_pos
                        }

                        else -> {
                            R.id.snabble_nav_payment_status
                        }
                    }
                }
            }

            CheckoutState.AUTHENTICATING -> R.id.snabble_nav_authentication

            CheckoutState.DEPOSIT_RETURN_REDEMPTION_FAILED,
            CheckoutState.PAYMENT_ABORTED -> {
                finish()
                null
            }

            CheckoutState.PAYMENT_ABORT_FAILED -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.Snabble_Payment_CancelError_title)
                    .setMessage(R.string.Snabble_Payment_CancelError_message)
                    .setPositiveButton(R.string.Snabble_ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .create()
                    .show()
                null
            }

            CheckoutState.PAYMENT_APPROVED -> {
                R.id.snabble_nav_payment_status
            }

            CheckoutState.NONE -> {
                finish()
                null
            }

            CheckoutState.PAYONE_SEPA_MANDATE_REQUIRED -> {
                R.id.snabble_nav_payment_payone_sepa_mandate
            }

            CheckoutState.PAYMENT_TRANSFERRED -> {
                R.id.snabble_nav_checkout_offline
            }

            else -> R.id.snabble_nav_payment_status
        }
    }

    private fun applyStatusBarStyling(color: Int) {
        val root = findViewById<View>(R.id.root)
        val windowInsetsController = WindowInsetsControllerCompat(window, root)
        setStatusBarColor(darkenColor(color))
        windowInsetsController.isAppearanceLightStatusBars = !isNightModeActive
    }

    private fun darkenColor(color: Int): Int {
        val hsv = FloatArray(HSV_COLOR_SIZE)
        Color.colorToHSV(color, hsv)
        hsv[2] *= (1f - DARKEN_FACTOR) // Reduce brightness
        return Color.HSVToColor(hsv)
    }

    private fun setStatusBarColor(color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // API 35+
            // For API 35+, use edge-to-edge but handle status bar coloring properly
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Create a status bar overlay instead of coloring the entire root view
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

                // Create a colored overlay just for the status bar area
                createStatusBarOverlay(color, statusBarInsets.top)

                insets
            }
        } else {
            // For API < 35, use the traditional method
            @Suppress("DEPRECATION")
            window.statusBarColor = color
        }
    }

    private var statusBarOverlay: View? = null

    private fun createStatusBarOverlay(color: Int, height: Int) {
        // Remove any existing overlay
        statusBarOverlay?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }

        // Create new overlay positioned absolutely at the top
        statusBarOverlay = View(this).apply {
            setBackgroundColor(color)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                height
            ).apply {
                gravity = Gravity.TOP // Position at the very top
            }
        }

        // Add overlay to the window's decor view
        val decorView = window.decorView as FrameLayout
        decorView.addView(statusBarOverlay)
    }

    private fun onStateChanged() {
        val navigationId = getNavigationId() ?: return
        val currentNavigationId = navController.currentDestination?.id
        if (currentNavigationId == navigationId) return

        navController.navigate(navigationId, null, NavOptions.Builder().apply {
            setPopUpTo(currentNavigationId ?: 0, true)
        }.build())
    }
}

val Context.isNightModeActive: Boolean
    get() {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else ->
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES
        }
    }

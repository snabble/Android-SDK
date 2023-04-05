package io.snabble.sdk.ui.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import io.snabble.sdk.InitializationState
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkout.Checkout
import io.snabble.sdk.checkout.CheckoutState
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.Logger

class CheckoutActivity : FragmentActivity() {
    companion object {

        const val ARG_PROJECT_ID = "projectId"

        @JvmStatic
        fun startCheckoutFlow(context: Context, newTask: Boolean = false) {
            val intent = Intent(context, CheckoutActivity::class.java)
            if (newTask) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        val showToolBar = resources.getBoolean(R.bool.showCheckoutToolbar)
        findViewById<View>(R.id.checkout_toolbar_spacer).isVisible = showToolBar
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            findViewById<View>(R.id.checkout_toolbar).isVisible = arguments?.getBoolean("showToolbar", false) == true
        }
        if (showToolBar) {
            applyInsets()
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
                        PaymentMethod.QRCODE_OFFLINE -> {
                            R.id.snabble_nav_checkout_offline
                        }
                        else -> {
                            R.id.snabble_nav_payment_status
                        }
                    }
                }
            }
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
            else -> R.id.snabble_nav_payment_status
        }
    }

    private fun applyInsets() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<View>(R.id.root)
        val windowInsetsController = WindowInsetsControllerCompat(window, root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            window.statusBarColor = Color.parseColor("#22000000")
            windowInsetsController.isAppearanceLightStatusBars = false

            val types = mutableListOf<Int>().apply {
                add(WindowInsetsCompat.Type.navigationBars())
                add(WindowInsetsCompat.Type.ime())
                add(WindowInsetsCompat.Type.systemBars())
                add(WindowInsetsCompat.Type.statusBars())
            }

            val currentInsetTypeMask = types.fold(0) { accumulator, type -> accumulator or type }

            @SuppressLint("WrongConstant")
            val insets = windowInsets.getInsets(currentInsetTypeMask)
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(insets.left, 0, insets.right, insets.bottom)
            }
            val toolbarSpacer = findViewById<View>(R.id.checkout_toolbar_spacer)
            toolbarSpacer.setPadding(0, insets.top, 0, 0)
            windowInsets.inset(insets.left, insets.top, insets.right, insets.bottom)
        }
    }

    override fun onBackPressed() {
        // prevent user from backing out while checkout is in progress
        if (checkout?.state?.value == CheckoutState.PAYMENT_APPROVED) {
            checkout?.reset()
            finish()
        }
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

package io.snabble.sdk.ui.checkout

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.fragment.NavHostFragment
import io.snabble.sdk.Checkout
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R

class CheckoutActivity : FragmentActivity() {
    companion object {
        const val ARG_PROJECT_ID = "projectId"
    }

    private lateinit var navGraph: NavGraph
    private lateinit var navController: NavController
    private lateinit var project: Project
    private var checkout: Checkout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.snabble_activity_checkout)

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_container
        ) as NavHostFragment
        val graphInflater = navHostFragment.navController.navInflater
        navGraph = graphInflater.inflate(R.navigation.snabble_nav_checkout)
        navController = navHostFragment.navController

        val projectId = intent.getStringExtra(ARG_PROJECT_ID)
        if (projectId == null) {
            finish()
            return
        }

        val project = Snabble.getInstance().getProjectById(projectId)
        if (project == null) {
            finish()
            return
        }

        val checkout = project.checkout
        // TODO handle more entry states?
        if (checkout.state == Checkout.State.NONE) {
            finish()
            return
        }
        this.checkout = checkout

        val startDestinationId = getNavigationId()
        if (startDestinationId == null) {
            finish()
            return
        } else {
            navGraph.setStartDestination(startDestinationId)
        }

        navController.graph = navGraph

        checkout.checkoutState.observe(this) {
            onStateChanged()
        }
    }

    private fun getNavigationId(): Int? {
        val checkout = checkout ?: return null

        if (checkout.state == Checkout.State.WAIT_FOR_APPROVAL) {
            val selectedPaymentMethod = checkout.selectedPaymentMethod
            selectedPaymentMethod?.let {
                when (it) {
                    PaymentMethod.TEGUT_EMPLOYEE_CARD,
                    PaymentMethod.DE_DIRECT_DEBIT,
                    PaymentMethod.VISA,
                    PaymentMethod.MASTERCARD,
                    PaymentMethod.AMEX,
                    PaymentMethod.PAYDIREKT,
                    PaymentMethod.TWINT,
                    PaymentMethod.POST_FINANCE_CARD,
                    PaymentMethod.GOOGLE_PAY -> {
                        return R.id.snabble_nav_checkout_online
                    }
                    PaymentMethod.GATEKEEPER_TERMINAL -> {
                        return R.id.snabble_nav_checkout_gatekeeper
                    }
                    PaymentMethod.QRCODE_POS -> {
                        return R.id.snabble_nav_checkout_pos
                    }
                    PaymentMethod.CUSTOMERCARD_POS -> {
                        return R.id.snabble_nav_checkout_customercard
                    }
                    PaymentMethod.QRCODE_OFFLINE -> {
                        return R.id.snabble_nav_checkout_offline
                    }
                }
            }
        } else {
            return R.id.snabble_nav_payment_status
        }

        return null
    }

    private fun onStateChanged() {
        val navigationId = getNavigationId() ?: return
        val currentNavigationId = navController.currentDestination?.id
        if (currentNavigationId == navigationId) return

        navController.navigate(navigationId,
            args = null,
            navOptions = NavOptions.Builder().apply {
                setPopUpTo(currentNavigationId ?: 0,
                    inclusive = true,
                    saveState = false
                )
            }.build())
    }
}
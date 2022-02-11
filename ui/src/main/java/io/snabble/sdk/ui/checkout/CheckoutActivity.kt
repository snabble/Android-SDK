package io.snabble.sdk.ui.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import io.snabble.sdk.*
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.utils.Logger

class CheckoutActivity : FragmentActivity() {
    companion object {
        const val ARG_PROJECT_ID = "projectId"

        @JvmStatic
        fun startCheckoutFlow(context: Context, args: Bundle?) {
            val intent = Intent(context, CheckoutActivity::class.java)
            args?.let {
                intent.putExtras(args)
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun startCheckoutFlow(context: Context, project: Project) {
            startCheckoutFlow(context, Bundle().apply {
                putString(ARG_PROJECT_ID, project.id)
            })
        }
    }

    private lateinit var navGraph: NavGraph
    private lateinit var navController: NavController
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

        Snabble.getInstance().initializationState.observe(this) {
            when(it) {
                InitializationState.INITIALIZED -> {
                    val projectId = intent.getStringExtra(ARG_PROJECT_ID)
                    if (projectId == null) {
                        finishWithError("No project id set")
                        return@observe
                    }

                    val project = Snabble.getInstance().getProjectById(projectId)
                    if (project == null) {
                        finishWithError("Project with id $projectId not found")
                        return@observe
                    }

                    val checkout = project.checkout
                    if (!checkout.state.isCheckoutState) {
                        finishWithError("Unexpected checkout state ${checkout.state.name}")
                        return@observe
                    }
                    this.checkout = checkout

                    val startDestinationId = getNavigationId()
                    if (startDestinationId == null) {
                        finish()
                        return@observe
                    } else {
                        navGraph.setStartDestination(startDestinationId)
                    }

                    navController.graph = navGraph

                    checkout.checkoutState.observe(this) {
                        onStateChanged()
                    }
                }
                else -> {}
            }
        }
    }

    private fun finishWithError(error: String) {
        Logger.e(error)
        finish()
    }

    private fun getNavigationId(): Int? {
        val checkout = checkout ?: return null

        return when(checkout.state) {
            Checkout.State.WAIT_FOR_GATEKEEPER -> {
                R.id.snabble_nav_routing_gatekeeper
            }
            Checkout.State.WAIT_FOR_SUPERVISOR -> {
                R.id.snabble_nav_routing_supervisor
            }
            Checkout.State.WAIT_FOR_APPROVAL -> {
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
            Checkout.State.PAYMENT_ABORTED -> {
                finish()
                null
            }
            Checkout.State.PAYMENT_APPROVED -> {
                R.id.snabble_nav_payment_status
            }
            Checkout.State.NONE -> {
                finish()
                null
            }
            else -> R.id.snabble_nav_payment_status
        }
    }

    override fun onBackPressed() {
        // prevent user from backing out
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
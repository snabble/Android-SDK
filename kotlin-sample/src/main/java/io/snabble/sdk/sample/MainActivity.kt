package io.snabble.sdk.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.integration.SelfScanningFragment
import io.snabble.sdk.ui.scanner.BarcodeDetector
import io.snabble.sdk.ui.scanner.BarcodeDetectorFactory
import io.snabble.sdk.ui.scanner.ZXingBarcodeDetector

class MainActivity : AppCompatActivity() {
    private lateinit var navView: BottomNavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // for simplicity sake we are reloading the whole application on state loss
        if (Snabble.getInstance().application == null) {
            startActivity(Intent(this, LoadingActivity::class.java))
            finish()
        } else {
            setContentView(R.layout.activity_main)
            toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            navView = findViewById(R.id.nav_view)

            val navController = findNavController(R.id.nav_host_fragment)

            val appBarConfiguration = AppBarConfiguration(setOf(
                    R.id.navigation_home,
                    R.id.navigation_scanner,
                    R.id.navigation_cart)
            )

            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)

            navController.addOnDestinationChangedListener { controller, destination, arguments ->
                when (destination.id) {
                    R.id.navigation_checkout_online -> setNavigationVisible(false)
                    R.id.navigation_checkout_customer_card_fragment -> setNavigationVisible(false)
                    R.id.navigation_checkout_gatekeeper_fragment -> setNavigationVisible(false)
                    else -> setNavigationVisible(true)
                }
            }

//            BarcodeDetectorFactory.setDefaultBarcodeDetectorFactory(object : BarcodeDetectorFactory() {
//                override fun create(): BarcodeDetector {
//                    return ZXingBarcodeDetector()
//                }
//            })
        }
    }

    override fun onBackPressed() {
        if (toolbar.isVisible) {
            super.onBackPressed()
        }
    }

    private fun setNavigationVisible(visible: Boolean) {
        if (visible) {
            toolbar.visibility = View.VISIBLE
            navView.visibility = View.VISIBLE
        } else {
            toolbar.visibility = View.GONE
            navView.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        val navController = findNavController(R.id.nav_host_fragment)
        with(navController) {
            SnabbleUI.registerUiCallbacks { action, data ->
                when(action) {
                    SnabbleUI.Action.SHOW_BARCODE_SEARCH -> {
                        navigate(R.id.navigation_barcode_search)
                    }
                    SnabbleUI.Action.SHOW_CHECKOUT_ONLINE -> {
                        navigate(R.id.navigation_checkout_online)
                    }
                    SnabbleUI.Action.SHOW_CHECKOUT_OFFLINE -> {
                        navigate(R.id.navigation_checkout_offline)
                    }
                    SnabbleUI.Action.SHOW_CHECKOUT_CUSTOMERCARD -> {
                        navigate(R.id.navigation_checkout_customer_card_fragment)
                    }
                    SnabbleUI.Action.SHOW_CHECKOUT_POINT_OF_SALE -> {
                        navigate(R.id.navigation_checkout_pos_fragment)
                    }
                    SnabbleUI.Action.SHOW_CHECKOUT_GATEKEEPER -> {
                        navigate(R.id.navigation_checkout_gatekeeper_fragment)
                    }
                    SnabbleUI.Action.SHOW_PAYMENT_SUCCESS -> {
                        navigate(R.id.navigation_action_success)
                    }
                    SnabbleUI.Action.SHOW_PAYMENT_FAILURE -> {
                        navigate(R.id.navigation_action_failure)
                    }
                    SnabbleUI.Action.SHOW_SCANNER -> {
                        val args = Bundle()
                        if (data != null) {
                            args.putString(SelfScanningFragment.ARG_SHOW_PRODUCT_CODE, data as String)
                        }
                        navigate(R.id.navigation_scanner, args)
                    }
                    SnabbleUI.Action.SHOW_SEPA_CARD_INPUT -> {
                        navigate(R.id.navigation_sepa_card_input)
                    }
                    SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT -> {
                        navigate(R.id.navigation_credit_card_input)
                    }
                    SnabbleUI.Action.SHOW_PAYDIREKT_INPUT -> {
                        navigate(R.id.navigation_paydirekt_input)
                    }
                    SnabbleUI.Action.SHOW_SHOPPING_CART -> {
                        navigate(R.id.navigation_cart)
                    }
                    SnabbleUI.Action.SHOW_PAYMENT_CREDENTIALS_LIST -> {
                        navigate(R.id.navigation_payment_credentials)
                    }
                    SnabbleUI.Action.SHOW_AGE_VERIFICATION -> {
                        navigate(R.id.navigation_age_verification)
                    }
                    SnabbleUI.Action.GO_BACK -> popBackStack()

                    // optional
                    SnabbleUI.Action.EVENT_PRODUCT_CONFIRMATION_SHOW -> {
                    }
                    SnabbleUI.Action.EVENT_PRODUCT_CONFIRMATION_HIDE -> {
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        SnabbleUI.registerUiCallbacks(null)
    }
}
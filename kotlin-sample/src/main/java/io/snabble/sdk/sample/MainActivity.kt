package io.snabble.sdk.sample

import android.content.Intent
import android.os.Bundle
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
import io.snabble.sdk.ui.checkout.CheckoutActivity

class MainActivity : AppCompatActivity() {
    private lateinit var navView: BottomNavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // for simplicity sake we are reloading the whole application on state loss
        if (Snabble.application == null) {
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
            toolbar.setNavigationOnClickListener { onBackPressed() }

            navController.addOnDestinationChangedListener { controller, destination, arguments ->
                when (destination.id) {
                    R.id.navigation_checkout_online -> setNavigationVisible(false)
                    R.id.navigation_checkout_customer_card_fragment -> setNavigationVisible(false)
                    R.id.navigation_checkout_gatekeeper_fragment -> setNavigationVisible(false)
                    else -> setNavigationVisible(true)
                }
            }

            with(navController) {
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_BARCODE_SEARCH) { _, _ ->
                    navigate(R.id.navigation_barcode_search)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_CHECKOUT) { context, args ->
                    CheckoutActivity.startCheckoutFlow(context, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_SCANNER) { _, args ->
                    navigate(R.id.navigation_scanner, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_SEPA_CARD_INPUT) { _, args ->
                    navigate(R.id.navigation_sepa_card_input, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT) { _, args ->
                    navigate(R.id.navigation_credit_card_input, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_PAYDIREKT_INPUT) { _, args ->
                    navigate(R.id.navigation_paydirekt_input, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_PAYONE_INPUT) { _, args ->
                    navigate(R.id.navigation_payone_input, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_SHOPPING_CART) { _, args ->
                    navigate(R.id.navigation_cart, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_PAYMENT_CREDENTIALS_LIST) { _, args ->
                    navigate(R.id.navigation_payment_credentials, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_AGE_VERIFICATION) { _, args ->
                    navigate(R.id.navigation_age_verification, args)
                }
                SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.GO_BACK) { _, _ ->
                    popBackStack()
                }
            }
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
}
package io.snabble.sdk.sample

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
import io.snabble.sdk.ui.SnabbleUI

class MainActivity : AppCompatActivity() {
    private lateinit var navView: BottomNavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        with(navController) {
            SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_BARCODE_SEARCH) { _, _ ->
                navigate(R.id.navigation_barcode_search)
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
            SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_AGE_VERIFICATION) { _, args ->
                navigate(R.id.navigation_age_verification, args)
            }
            SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.GO_BACK) { _, _ ->
                popBackStack()
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
package io.snabble.sdk.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkin.OnCheckInStateChangedListener
import io.snabble.sdk.ui.SnabbleUI

class MainActivity : AppCompatActivity() {
    private lateinit var navView: BottomNavigationView
    private lateinit var toolbar: Toolbar

    lateinit var locationPermission: ActivityResultLauncher<String>

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
                R.id.navigation_cart,
//                R.id.navigation_dummy_cart
        ))

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        with(navController) {
            SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_BARCODE_SEARCH) { _, args ->
                navigate(R.id.navigation_barcode_search, args)
            }
            SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_SCANNER) { _, args ->
                navigate(R.id.navigation_scanner, args)
            }
            SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_SHOPPING_CART) { _, args ->
                 navigate(R.id.navigation_cart, args)
//                navigate(R.id.navigation_dummy_cart, args)
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
            SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_AGE_VERIFICATION) { _, args ->
                navigate(R.id.navigation_age_verification, args)
            }
            SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.GO_BACK) { _, _ ->
                popBackStack()
            }
            SnabbleUI.setUiAction(this@MainActivity,SnabbleUI.Event.NOT_CHECKED_IN){_,_->
                navigate(R.id.not_checked_in)
            }
        }

        locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Snabble.checkInManager.startUpdating()
                }
            } else {
                Snabble.checkInManager.stopUpdating()
            }
        }

        // add a check in state listener to observe when a user enters or leaves a shop
        Snabble.checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                Toast.makeText(this@MainActivity, "Check in: " + shop.name, Toast.LENGTH_LONG).show()
            }

            override fun onCheckOut() {
                Toast.makeText(this@MainActivity, "Check out", Toast.LENGTH_LONG).show()
            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {
                // if multiple shops are in range a list will be provided
                // a valid implementation of this can be just doing nothing
                // as this will use the first shop (the nearest) of the list and stick to it
                //
                // a proper implementation would hint the user to select the shop he is currently in
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
         || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Snabble.checkInManager.startUpdating()
        }
    }

    override fun onPause() {
        super.onPause()
        Snabble.checkInManager.stopUpdating()
    }
}
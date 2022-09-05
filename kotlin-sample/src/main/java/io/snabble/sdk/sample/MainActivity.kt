package io.snabble.sdk.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.checkin.CheckInLocationManager
import io.snabble.sdk.checkin.OnCheckInStateChangedListener
import io.snabble.sdk.onboarding.entities.OnboardingModel
import io.snabble.sdk.ui.SnabbleUI

class MainActivity : AppCompatActivity() {
    private lateinit var navView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var locationManager: CheckInLocationManager

    lateinit var locationPermission: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = Snabble.checkInLocationManager
        // start location tracking after permission is granted
        locationManager.startTrackingLocation()

        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        navView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_scanner,
                R.id.navigation_cart,
                R.id.navigation_shops
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        //Onboarding Example:
        //1. converting the json config file with Gson into the OnboardingModel object
        //2. starting the Onboarding via the SnabbleUi action and passing the model as arg
        if (savedInstanceState == null) {
            val json = resources.assets.open("onboardingConfig.json").bufferedReader().readText()
            val model = Gson().fromJson(json, OnboardingModel::class.java)
            SnabbleUiToolkit.executeAction(
                this,
                SnabbleUiToolkit.Event.SHOW_ONBOARDING,
                bundleOf("model" to model)
            )
        }

        // Can be used to get args from deeplinks. In this case the args are used to
        // enable/disable the toolbar and navbar for each page separately
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            println("Nav to ${resources.getResourceName(destination.id)}")
            // apply deeplink arguments to bundle
            if (arguments?.containsKey(NavController.KEY_DEEP_LINK_INTENT) == true) {
                (arguments.get(NavController.KEY_DEEP_LINK_INTENT) as? Intent)?.data?.let { deeplink ->
                    deeplink.queryParameterNames.forEach { key ->
                        val value = deeplink.getQueryParameter(key)
                        when {
                            value?.toIntOrNull() != null -> arguments.putInt(key, value.toInt())
                            value?.toLongOrNull() != null -> arguments.putLong(key, value.toLong())
                            value?.toBooleanStrictOrNull() != null -> arguments.putBoolean(key, value.toBoolean())
                            else -> arguments.putString(key, deeplink.getQueryParameter(key))
                        }
                    }
                }
            }
            toolbar.isVisible = arguments?.getBoolean("hideToolbar", false) != true
            navView.isVisible = arguments?.getBoolean("hideBottomNavigation", false) != true
            navView.isEnabled = arguments?.getBoolean("hideBottomNavigation", false) != true
            toolbar.title = destination.label
            arguments?.getString("title")?.let {
                toolbar.title = it.replace("...", "…")
            }
        }

        SnabbleUI.setUiAction(this, SnabbleUI.Event.SHOW_BARCODE_SEARCH) { _, args ->
            navController.navigate(R.id.navigation_barcode_search, args)
        }
        SnabbleUI.setUiAction(this, SnabbleUI.Event.SHOW_SCANNER) { _, args ->
            navController.navigate(R.id.navigation_scanner, args)
        }
        SnabbleUI.setUiAction(this, SnabbleUI.Event.SHOW_SHOPPING_CART) { _, args ->
            navController.navigate(R.id.navigation_cart, args)
        }
        SnabbleUI.setUiAction(this, SnabbleUI.Event.SHOW_SEPA_CARD_INPUT) { _, args ->
            navController.navigate(R.id.navigation_sepa_card_input, args)
        }
        SnabbleUI.setUiAction(this, SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT) { _, args ->
            navController.navigate(R.id.navigation_credit_card_input, args)
        }
        SnabbleUI.setUiAction(this, SnabbleUI.Event.SHOW_PAYDIREKT_INPUT) { _, args ->
            navController.navigate(R.id.navigation_paydirekt_input, args)
        }
        SnabbleUI.setUiAction(this, SnabbleUI.Event.SHOW_PAYONE_INPUT) { _, args ->
            navController.navigate(R.id.navigation_payone_input, args)
        }
        SnabbleUI.setUiAction(this, SnabbleUI.Event.SHOW_AGE_VERIFICATION) { _, args ->
            navController.navigate(R.id.navigation_age_verification, args)
        }
        SnabbleUI.setUiAction(this, SnabbleUI.Event.GO_BACK) { _, _ ->
            navController.popBackStack()
        }
        SnabbleUiToolkit.setUiAction(this, SnabbleUiToolkit.Event.SHOW_DETAILS_SHOP_LIST) { _, args ->
            navController.navigate(R.id.navigation_shops_details, args)
        }
        SnabbleUiToolkit.setUiAction(this, SnabbleUiToolkit.Event.DETAILS_SHOP_BUTTON_ACTION) { _, _ ->
            navView.selectedItemId = R.id.navigation_scanner
        }

        // listens to permission result and start tracking if permission is granted
        locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (isHoldingPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    || isHoldingPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    // noinspection MissingPermission
                    Snabble.checkInManager.startUpdating()
                    locationManager.startTrackingLocation()
                } else {
                    locationManager.stopTrackingLocation()
                    Snabble.checkInManager.stopUpdating()
                }
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
                // a proper implementation would hint the user to select the shop he is currently in
            }
        })
    }

    private fun isHoldingPermission(permission: String): Boolean =
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onResume() {
        super.onResume()

        if (isHoldingPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            || isHoldingPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // noinspection MissingPermission
            Snabble.checkInManager.startUpdating()
        }
    }

    override fun onPause() {
        super.onPause()
        Snabble.checkInManager.stopUpdating()
    }

    // Check in example if the "Snabble" "CheckInManager" is not used, otherwise it is handled
    @Suppress("unused")
    fun checkIn() {
        val shopList = Snabble.projects[0].shops
        // CheckinRadius in Meters
        val checkInRadius = 15.0f

        // Observe the current location via the locationManager to track if a shop matches
        // the check in radius. If yes check in.
        locationManager.location.observe(this) { currentLocation ->
            val nearestshop = currentLocation?.let { location ->
                shopList.firstOrNull { it.location.distanceTo(location) < checkInRadius }
            }

            // Set shop and project on check in
            if (nearestshop != null) {
                Snabble.checkedInShop = nearestshop
                Snabble.checkedInProject.setValue(Snabble.projects.first())
            } else {
                Snabble.checkedInShop = null
                Snabble.checkedInProject.setValue(null)
            }
        }
    }
}
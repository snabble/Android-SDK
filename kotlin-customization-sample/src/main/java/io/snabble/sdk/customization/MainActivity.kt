package io.snabble.sdk.customization

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.checkin.OnCheckInStateChangedListener
import io.snabble.sdk.customization.repository.OnboardingRepository
import io.snabble.sdk.customization.repository.OnboardingRepositoryImpl
import io.snabble.sdk.screens.onboarding.data.OnboardingModel
import io.snabble.sdk.ui.SnabbleUI
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var navView: BottomNavigationView
    private lateinit var toolbar: Toolbar

    lateinit var locationPermission: ActivityResultLauncher<String>

    private val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    private var showOnboarding: Boolean?
        get() {
            return if (sharedPreferences.contains(PREF_KEY_SHOW_ONBOARDING)) {
                sharedPreferences.getBoolean(PREF_KEY_SHOW_ONBOARDING, true)
            } else {
                null
            }
        }
        set(showOnboardingSeen) {
            if (showOnboardingSeen != null) {
                sharedPreferences.edit()
                    .putBoolean(PREF_KEY_SHOW_ONBOARDING, showOnboardingSeen)
                    .apply()
            } else {
                sharedPreferences.edit().remove(PREF_KEY_SHOW_ONBOARDING).apply()
            }
        }

    private val onboardingRepo: OnboardingRepository by lazy {
        OnboardingRepositoryImpl(assets, Gson())
    }

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
                R.id.navigation_cart
        ))

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        if (savedInstanceState == null && showOnboarding != false) {
            lifecycleScope.launch {
                val model = onboardingRepo.getOnboardingModel()
                SnabbleUiToolkit.executeAction(
                    context = this@MainActivity,
                    SnabbleUiToolkit.Event.SHOW_ONBOARDING,
                    bundleOf(getString(R.string.bundle_key_model) to model)
                )
            }
        }

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            println("Nav to ${resources.getResourceName(destination.id)}")
            // apply deeplink arguments to bundle
            if (arguments?.containsKey(NavController.KEY_DEEP_LINK_INTENT) == true) {
                arguments.parcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)?.data?.let { deeplink ->
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
                Snabble.checkInManager.project?.let {
                    Snabble.checkedInProject.value = it
                }

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

    companion object {

        const val PREF_KEY_SHOW_ONBOARDING = "snabble.show.onboarding"
    }
}

private inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

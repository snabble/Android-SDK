package io.snabble.sdk.sample

import android.Manifest
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.checkin.OnCheckInStateChangedListener
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepository
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepositoryImpl
import io.snabble.sdk.ui.SnabbleUI
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var locationPermission: ActivityResultLauncher<String>

    private val onboardingRepo: OnboardingRepository by lazy {
        OnboardingRepositoryImpl(assets, Gson())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.nav_host_fragment)
        val navBarView: NavigationBarView = findViewById(R.id.nav_view)
        navBarView.setupWithNavController(navController)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setupToolbar(toolbar, navController, navBarView)

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val model = onboardingRepo.getOnboardingModel()
                SnabbleUiToolkit.executeAction(
                    context = this@MainActivity,
                    SnabbleUiToolkit.Event.SHOW_ONBOARDING,
                    bundleOf(getString(R.string.bundle_key_model) to model)
                )
            }
        }

        locationPermission = createLocationPermissionRequestResultLauncher()
        addSnabbleSdkCheckInListener()
    }

    override fun onResume() {
        super.onResume()

        startCheckInManagerUpdating()
    }

    override fun onPause() {
        super.onPause()
        Snabble.checkInManager.stopUpdating()
    }

    private fun addSnabbleSdkCheckInListener() {
        // add a check in state listener to observe when a user enters or leaves a shop
        Snabble.checkInManager.addOnCheckInStateChangedListener(
            object : OnCheckInStateChangedListener {

                override fun onCheckIn(shop: Shop) {
                    Toast.makeText(this@MainActivity, "Check in: " + shop.name, Toast.LENGTH_LONG)
                        .show()
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
            }
        )
    }

    private fun createLocationPermissionRequestResultLauncher(): ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCheckInManagerUpdating()
            } else {
                Snabble.checkInManager.stopUpdating()
            }
        }

    private fun startCheckInManagerUpdating() {
        val hasLocationFinePermission = ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasLocationCoarsePermission = ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocationFinePermission || hasLocationCoarsePermission) {
            Snabble.checkInManager.startUpdating()
        }
    }

    private fun NavController.setup(toolbar: Toolbar, navBarView: NavigationBarView) {
        addOnDestinationChangedListener { _, destination, arguments ->
            println("Nav to ${resources.getResourceName(destination.id)}")
            toolbar.isVisible = arguments?.getBoolean("hideToolbar", false) != true
            navBarView.isVisible = arguments?.getBoolean("hideBottomNavigation", false) != true
            navBarView.isEnabled = arguments?.getBoolean("hideBottomNavigation", false) != true
            toolbar.title = destination.label
            arguments?.getString("title")?.let {
                toolbar.title = it.replace("...", "…")
            }
        }

        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.SHOW_BARCODE_SEARCH) { _, args ->
            navigate(R.id.navigation_barcode_search, args)
        }
        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.SHOW_SCANNER) { _, args ->
            navigate(R.id.navigation_scanner, args)
        }
        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.SHOW_SHOPPING_CART) { _, args ->
            navigate(R.id.navigation_cart, args)
            // navigate(R.id.navigation_dummy_cart, args)
        }
        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.SHOW_SEPA_CARD_INPUT) { _, args ->
            navigate(R.id.navigation_sepa_card_input, args)
        }
        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT) { _, args ->
            navigate(R.id.navigation_credit_card_input, args)
        }
        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.SHOW_PAYDIREKT_INPUT) { _, args ->
            navigate(R.id.navigation_paydirekt_input, args)
        }
        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.SHOW_PAYONE_INPUT) { _, args ->
            navigate(R.id.navigation_payone_input, args)
        }
        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.SHOW_AGE_VERIFICATION) { _, args ->
            navigate(R.id.navigation_age_verification, args)
        }
        SnabbleUI.setUiAction(this@MainActivity,
            SnabbleUI.Event.GO_BACK) { _, _ ->
            popBackStack()
        }
    }

    private fun setupToolbar(
        toolbar: Toolbar,
        navController: NavController,
        navBarView: NavigationBarView,
    ) {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        navController.setup(toolbar, navBarView)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_scanner,
                R.id.navigation_cart,
                // R.id.navigation_dummy_cart,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }
}

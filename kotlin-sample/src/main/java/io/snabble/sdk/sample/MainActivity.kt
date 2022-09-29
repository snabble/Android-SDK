package io.snabble.sdk.sample

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.checkin.CheckInLocationManager
import io.snabble.sdk.checkin.OnCheckInStateChangedListener
import io.snabble.sdk.home.HomeViewModel
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepository
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepositoryImpl
import io.snabble.sdk.sample.utils.PermissionSupport
import io.snabble.sdk.ui.SnabbleUI
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), PermissionSupport {

    private lateinit var locationPermission: ActivityResultLauncher<String>

    private lateinit var locationManager: CheckInLocationManager

    private val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    private var onboardingSeen: Boolean
        get() = sharedPreferences.getBoolean(ONBOARDING_SEEN, false)
        set(onboardingSeen) = sharedPreferences.edit()
            .putBoolean(ONBOARDING_SEEN, onboardingSeen)
            .apply()

    private val viewModel: HomeViewModel by viewModels()

    private val onboardingRepo: OnboardingRepository by lazy {
        OnboardingRepositoryImpl(assets, Gson())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = Snabble.checkInLocationManager
        // start location tracking after permission is granted
        locationManager.startTrackingLocation()

        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.nav_host_fragment)
        val navBarView: NavigationBarView = findViewById(R.id.nav_view)
        navBarView.setupWithNavController(navController)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setupToolbar(toolbar, navController, navBarView)

        viewModel.widgetEvent.observe(this) { event ->
            when (event) {
                "location" -> startLocationPermissionRequest()
                "start" -> navBarView.selectedItemId = R.id.navigation_cart
                "stores" -> navBarView.selectedItemId = R.id.navigation_shop
            }
        }

        if (savedInstanceState == null && !onboardingSeen) {
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

    override fun startLocationPermissionRequest() {
        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
            viewModel.permissionState.value = true
        }

    private fun startCheckInManagerUpdating() {
        val hasLocationFinePermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasLocationCoarsePermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLocationFinePermission || hasLocationCoarsePermission) {
            Snabble.checkInManager.startUpdating()
        }
    }

    // Can be used to get args from deeplinks. In this case the args are used to
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

        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.SHOW_BARCODE_SEARCH
        ) { _, args ->
            navigate(R.id.navigation_barcode_search, args)
        }
        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.SHOW_SCANNER
        ) { _, args ->
            navigate(R.id.navigation_scanner, args)
        }
        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.SHOW_SHOPPING_CART
        ) { _, args ->
            navigate(R.id.navigation_cart, args)
            // navigate(R.id.navigation_dummy_cart, args)
        }
        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.SHOW_SEPA_CARD_INPUT
        ) { _, args ->
            navigate(R.id.navigation_sepa_card_input, args)
        }
        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT
        ) { _, args ->
            navigate(R.id.navigation_credit_card_input, args)
        }
        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.SHOW_PAYDIREKT_INPUT
        ) { _, args ->
            navigate(R.id.navigation_paydirekt_input, args)
        }
        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.SHOW_PAYONE_INPUT
        ) { _, args ->
            navigate(R.id.navigation_payone_input, args)
        }
        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.SHOW_AGE_VERIFICATION
        ) { _, args ->
            navigate(R.id.navigation_age_verification, args)
        }
        SnabbleUI.setUiAction(
            this@MainActivity,
            SnabbleUI.Event.GO_BACK
        ) { _, _ ->
        }
        SnabbleUiToolkit.setUiAction(
            this@MainActivity,
            SnabbleUiToolkit.Event.SHOW_DETAILS_SHOP_LIST
        ) { _, args ->
            navigate(R.id.navigation_shops_details, args)
        }
        SnabbleUiToolkit.setUiAction(
            this@MainActivity,
            SnabbleUiToolkit.Event.DETAILS_SHOP_BUTTON_ACTION
        ) { _, _ ->
            navBarView.selectedItemId = R.id.navigation_scanner
        }
        SnabbleUiToolkit.setUiAction(
            this@MainActivity,
            SnabbleUiToolkit.Event.SHOW_DEEPLINK
        ) { _, args ->
            val uri = Uri.parse(requireNotNull(args?.getString(SnabbleUiToolkit.DEEPLINK)))
            navigate(NavDeepLinkRequest.Builder.fromUri(uri).build())
        }
        SnabbleUiToolkit.setUiAction(
            this@MainActivity,
            SnabbleUiToolkit.Event.SHOW_ONBOARDING
        ) { _, args ->
            navigate(R.id.frag_onboarding, args)
        }
        SnabbleUiToolkit.setUiAction(
            this@MainActivity,
            SnabbleUiToolkit.Event.SHOW_ONBOARDING_DONE
        ) { _, _ ->
            popBackStack()
            onboardingSeen = true
        }

        // listens to permission result and start tracking if permission is granted
        locationPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    if (isHoldingPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        || isHoldingPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    ) {
                        // noinspection MissingPermission
                        Snabble.checkInManager.startUpdating()
                        locationManager.startTrackingLocation()
                    } else {
                        locationManager.stopTrackingLocation()
                        Snabble.checkInManager.stopUpdating()
                    }
                }
            }
    }

    private fun isHoldingPermission(permission: String): Boolean =
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

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
                R.id.navigation_shops,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
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

    companion object {
        private const val ONBOARDING_SEEN = "onboarding_seen"
    }
}

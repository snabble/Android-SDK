package io.snabble.sdk.sample

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.home.viewmodel.DynamicHomeViewModel
import io.snabble.sdk.home.viewmodel.DynamicProfileViewModel
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepository
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepositoryImpl
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.utils.xx
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    private var onboardingSeen: Boolean
        get() = sharedPreferences.getBoolean(ONBOARDING_SEEN, false)
        set(onboardingSeen) = sharedPreferences.edit()
            .putBoolean(ONBOARDING_SEEN, onboardingSeen)
            .apply()

    private val homeViewModel: DynamicHomeViewModel by viewModels()
    private val profileViewModel: DynamicProfileViewModel by viewModels()

    private val onboardingRepo: OnboardingRepository by lazy {
        OnboardingRepositoryImpl(assets, Gson())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navBarView: NavigationBarView = findViewById(R.id.nav_view)

        navBarView.setupWithNavController(navController)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setupToolbar(toolbar, navController, navBarView)

        profileViewModel.xx("MainActivity:").actions.asLiveData()
            .observe(this) { action ->
                when (action.widget.id) {
                    else -> action.xx("ProfileAction")
                }

            }
        homeViewModel.xx("MainActivity:").actions.asLiveData()
            .observe(this) { action ->
                when (action.widget.id) {
                    "location" -> {}
                    "start" -> navBarView.selectedItemId = R.id.navigation_cart
                    "stores" -> {
                        SnabbleUiToolkit.executeAction(this, SnabbleUiToolkit.Event.SHOW_SHOP_LIST)
                    }
                    else -> action.xx("DynamicAction ->")
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
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()

        Snabble.checkInManager.startUpdating()
    }

    override fun onPause() {
        super.onPause()

        Snabble.checkInManager.stopUpdating()
    }

    // Can be used to get args from deeplinks. In this case the args are used to
    private fun NavController.setup(toolbar: Toolbar, navBarView: NavigationBarView) {
        addOnDestinationChangedListener { _, destination, arguments ->
            arguments.xx("Nav to ${resources.getResourceName(destination.id)}")
            toolbar.isVisible = arguments?.getBoolean("hideToolbar") != true
            val isBottomNavigationVisible = arguments?.getBoolean("hideBottomNavigation") != true
            navBarView.isVisible = isBottomNavigationVisible
            navBarView.isEnabled = isBottomNavigationVisible
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

    }

    private fun setupToolbar(
        toolbar: Toolbar,
        navController: NavController,
        navBarView: NavigationBarView,
    ) {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        navBarView.setOnItemReselectedListener {
            // No action needed on re-selecting
        }

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
//    @Suppress("unused")
//    fun checkIn() {
//        val shopList = Snabble.projects[0].shops
//        // CheckinRadius in Meters
//        val checkInRadius = 15.0f
//
//        // Observe the current location via the locationManager to track if a shop matches
//        // the check in radius. If yes check in.
//        locationManager.location.observe(this) { currentLocation ->
//            val nearestshop = currentLocation?.let { location ->
//                shopList.firstOrNull { it.location.distanceTo(location) < checkInRadius }
//            }
//
//            // Set shop and project on check in
//            if (nearestshop != null) {
//                Snabble.checkedInShop = nearestshop
//                Snabble.checkedInProject.setValue(Snabble.projects.first())
//            } else {
//                Snabble.checkedInShop = null
//                Snabble.checkedInProject.setValue(null)
//            }
//        }
//    }

    companion object {

        private const val ONBOARDING_SEEN = "onboarding_seen"
    }
}

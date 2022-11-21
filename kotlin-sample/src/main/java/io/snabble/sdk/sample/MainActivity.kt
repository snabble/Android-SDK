package io.snabble.sdk.sample

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationBarView
import com.google.gson.Gson
import io.snabble.sdk.Snabble
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.SnabbleUiToolkit.Event.SHOW_RECEIPT_LIST
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepository
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepositoryImpl
import io.snabble.sdk.screens.home.viewmodel.DynamicHomeViewModel
import io.snabble.sdk.screens.profile.viewmodel.DynamicProfileViewModel
import io.snabble.sdk.screens.receipts.ReceiptProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

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
        setUpUiEvents(this, navController, navBarView)

        profileViewModel.actions.asLiveData()
            .observe(this) { action ->
                when (action.widget.id) {
                    "show.lastPurchases" -> SnabbleUiToolkit.executeAction(context = this, SHOW_RECEIPT_LIST)
                    else -> Unit
                }
            }

        homeViewModel.actions.asLiveData()
            .observe(this) { action ->
                when (action.widget.id) {
                    "start" -> navBarView.selectedItemId = R.id.navigation_scanner
                    "stores" -> navBarView.selectedItemId = R.id.navigation_shop
                    "purchases" -> {
                        when (action.info?.get("action")) {
                            "more" -> SnabbleUiToolkit.executeAction(context = this, SHOW_RECEIPT_LIST)

                            "purchase" -> {
                                (action.info?.get("id") as? String)?.let {
                                    val receiptProvider = ReceiptProvider(this)
                                    lifecycleScope.launch {

                                        receiptProvider.showReceipt(it)
                                    }
//                                    showDetails(it, this)
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }

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
            "Nav to ${resources.getResourceName(destination.id)}"
            toolbar.isVisible = arguments?.getBoolean("hideToolbar") != true
            val isBottomNavigationVisible = arguments?.getBoolean("hideBottomNavigation") != true
            navBarView.isVisible = isBottomNavigationVisible
            navBarView.isEnabled = isBottomNavigationVisible
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
                R.id.navigation_shops,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    companion object {

        const val PREF_KEY_SHOW_ONBOARDING = "snabble.show.onboarding"
    }
}

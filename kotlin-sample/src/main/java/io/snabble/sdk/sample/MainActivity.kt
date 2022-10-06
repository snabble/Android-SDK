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
import io.snabble.sdk.home.viewmodel.DynamicHomeViewModel
import io.snabble.sdk.home.viewmodel.DynamicProfileViewModel
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepository
import io.snabble.sdk.sample.onboarding.repository.OnboardingRepositoryImpl
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
        setUpUiEvents(this, navController, navBarView)

        profileViewModel.xx("MainActivity:").actions.asLiveData()
            .observe(this) { action ->
                when (action.widget.id) {
                    else -> action.xx("ProfileAction")
                }

            }
        homeViewModel.xx("MainActivity:").actions.asLiveData()
            .observe(this) { action ->
                when (action.widget.id) {
                    "start" -> navBarView.selectedItemId = R.id.navigation_scanner
                    "stores" -> navBarView.selectedItemId = R.id.navigation_shop
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

    companion object {

        const val ONBOARDING_SEEN = "onboarding_seen"
    }
}

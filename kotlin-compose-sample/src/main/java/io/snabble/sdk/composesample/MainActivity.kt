package io.snabble.sdk.composesample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.snabble.sdk.InitializationState
import io.snabble.sdk.Snabble
import io.snabble.sdk.composesample.screens.Cart
import io.snabble.sdk.composesample.screens.Home
import io.snabble.sdk.composesample.screens.Scanner

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // select the first shop for demo purposes
        Snabble.initializationState.observe(this) {
            if (it == InitializationState.INITIALIZED) {
                Snabble.checkedInShop = Snabble.projects.first().shops.first()
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                val navController = rememberNavController()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Scaffold(
                    topBar = {
                        SmallTopAppBar(
                            title = { Text(currentDestination?.label?.toString().orEmpty()) },
                        )
                    },
                    bottomBar = {
                        BottomBar(
                            navController = navController
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = BottomNavigationItem.Home.route,
                        modifier = Modifier.padding(innerPadding)) {
                            composable(BottomNavigationItem.Home.route) {
                                it.destination.label = BottomNavigationItem.Home.title
                                Home()
                            }
                            composable(BottomNavigationItem.Scanner.route) {
                                it.destination.label = BottomNavigationItem.Scanner.title
                                Scanner()
                            }
                            composable(BottomNavigationItem.Cart.route) {
                                it.destination.label = BottomNavigationItem.Cart.title
                                Cart()
                            }
                    }
                }
            }
        }
    }

    @Composable
    fun BottomBar(navController: NavController) {
        val items = listOf(
            BottomNavigationItem.Home,
            BottomNavigationItem.Scanner,
            BottomNavigationItem.Cart,
        )

        NavigationBar {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(painterResource(item.icon), null) },
                    label = { Text(item.title) },
                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                            actionBar?.title = item.title
                        }
                    }
                )
            }
        }
    }
}
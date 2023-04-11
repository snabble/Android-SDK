package io.snabble.sdk.composesample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.snabble.sdk.InitializationState
import io.snabble.sdk.Snabble
import io.snabble.sdk.composesample.screens.BarcodeSearch
import io.snabble.sdk.composesample.screens.Cart
import io.snabble.sdk.composesample.screens.Home
import io.snabble.sdk.composesample.screens.Scanner
import io.snabble.sdk.ui.SnabbleUI

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {

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

                with(navController) {
                    SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_BARCODE_SEARCH) { _, _ ->
                        navigate(BarcodeSearch.route)
                    }
                    SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_SCANNER) { _, _ ->
                        navigate(Scanner.route)
                    }
                    SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.SHOW_SHOPPING_CART) { _, _ ->
                        navigate(Cart.route)
                    }
                    SnabbleUI.setUiAction(this@MainActivity, SnabbleUI.Event.GO_BACK) { _, _ ->
                        popBackStack()
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(currentDestination?.label?.toString().orEmpty()) },
                            actions = {
                                IconButton(
                                    content = { Icon(Icons.Filled.Search, "Search") },
                                    onClick = {
                                        navController.navigate("barcodeSearch")
                                    }
                                )
                            })
                    },
                    bottomBar = {
                        BottomBar(
                            navController = navController
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Home.route) {
                            it.destination.label = Home.title
                            Home()
                        }
                        composable(Scanner.route) {
                            it.destination.label = Scanner.title
                            Scanner()
                        }
                        composable(Cart.route) {
                            it.destination.label = Cart.title
                            Cart()
                        }
                        composable(BarcodeSearch.route) {
                            it.destination.label = BarcodeSearch.title
                            BarcodeSearch()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BottomBar(navController: NavController) {
        val items = listOf(
            BottomNavigationItem.MenuHome,
            BottomNavigationItem.MenuScanner,
            BottomNavigationItem.MenuCart,
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

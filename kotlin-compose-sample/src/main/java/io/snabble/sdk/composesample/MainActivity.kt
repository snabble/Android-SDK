package io.snabble.sdk.composesample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.snabble.sdk.InitializationState
import io.snabble.sdk.Snabble
import io.snabble.sdk.composesample.screens.Cart
import io.snabble.sdk.composesample.screens.Home
import io.snabble.sdk.composesample.screens.Scanner

class MainActivity : FragmentActivity() {
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Snabble.initializationState.observe(this) {
            if (it == InitializationState.INITIALIZED) {
                Snabble.checkedInShop = Snabble.projects.first().shops.first()
            }
        }

        setContent {
            MaterialTheme {
                Cart()
            }
        }
    }
    
    @Composable
    fun BottomNavigation() {
        var selectedItem by remember { mutableStateOf(0) }

        val items = listOf(
            BottomNavigationItem.Home,
            BottomNavigationItem.Scanner,
            BottomNavigationItem.Cart,
        )

        NavigationBar {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = { item.icon },
                    label = { item.title },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
    }

    @Composable
    fun NavigationGraph(navController: NavHostController) {
        NavHost(navController, startDestination = BottomNavigationItem.Home.id) {
            composable(BottomNavigationItem.Home.id) {
                Home()
            }
            composable(BottomNavigationItem.Scanner.id) {
                Scanner()
            }
            composable(BottomNavigationItem.Cart.id) {
                Cart()
            }
        }
    }
}
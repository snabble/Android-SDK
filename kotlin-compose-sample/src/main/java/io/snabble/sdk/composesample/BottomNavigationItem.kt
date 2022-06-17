package io.snabble.sdk.composesample

import io.snabble.sdk.composesample.screens.Cart
import io.snabble.sdk.composesample.screens.Home
import io.snabble.sdk.composesample.screens.Scanner

sealed class BottomNavigationItem(var title: String,
                                  var icon: Int,
                                  var route: String){
    object MenuHome : BottomNavigationItem(
        title = Home.title,
        icon = R.drawable.ic_baseline_home_24,
        route = Home.route
    )

    object MenuScanner: BottomNavigationItem(
        title = Scanner.title,
        icon = R.drawable.ic_baseline_qr_code_scanner_24,
        route = Scanner.route
    )

    object MenuCart: BottomNavigationItem(
        title = Cart.title,
        icon = R.drawable.ic_baseline_shopping_cart_24,
        route = Cart.route
    )
}
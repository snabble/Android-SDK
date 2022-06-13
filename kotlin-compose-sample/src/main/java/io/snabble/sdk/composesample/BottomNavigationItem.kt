package io.snabble.sdk.composesample

sealed class BottomNavigationItem(var title: String,
                                  var icon: Int,
                                  var id: String){
    object Home : BottomNavigationItem(
        title = "Home",
        icon = R.drawable.ic_baseline_home_24,
        id = "home"
    )

    object Scanner: BottomNavigationItem(
        title = "Scanner",
        icon = R.drawable.ic_baseline_qr_code_scanner_24,
        id = "scanner"
    )

    object Cart: BottomNavigationItem(
        title = "Cart",
        icon = R.drawable.ic_baseline_shopping_cart_24,
        id = "cart"
    )
}
package io.snabble.sdk.screens.shopfinder.shoplist

enum class ViewType(val isShop: Boolean) {
    CollapsedBrand(false),
    ExpandedBrand(false),
    HiddenShop(true),
    VisibleShop(true);

    val isBrand: Boolean
        get() = !isShop
}

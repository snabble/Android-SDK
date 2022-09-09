package io.snabble.sdk.shopfinder.shoplist

enum class ViewType(val isShop: Boolean) {
    CollapsedBrand(false),
    ExpandedBrand(false),
    HiddenShop(true),
    VisibleShop(true);

    val isBrand: Boolean
        get() = !isShop
}

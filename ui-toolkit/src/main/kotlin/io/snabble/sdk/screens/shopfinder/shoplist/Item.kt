package io.snabble.sdk.screens.shopfinder.shoplist

import android.location.Location
import io.snabble.sdk.Project
import io.snabble.sdk.Shop
import io.snabble.sdk.screens.shopfinder.utils.formatDistance

data class Item(
    val id: String,
    val project: Project,
    var location: Location,
    val name: String,
    var type: ViewType,
    var isCheckedIn: Boolean = false,
    val shops: Int? = null, // null for Shops
    val street: String? = null, // null for a SectionHeader
    val zipCode: String? = null, // null for a SectionHeader
    val city: String? = null, // null for a SectionHeader
    var distance: Float? = null // null without location
) : Comparable<Item> {
    constructor(project: Project) : this(
        id = project.id,
        project = project,
        location = project.shops.first().location,
        name = project.brand?.name ?: project.name,
        type = ViewType.CollapsedBrand,
        shops = project.shops.size
    )

    constructor(shop: Shop, project: Project) : this(
        id = shop.id,
        project = project,
        location = shop.location,
        name = shop.name,
        type = ViewType.HiddenShop,
        street = shop.street,
        zipCode = shop.zipCode,
        city = shop.city
    )

    val address: String?
        get() =
            if (street != null && city != null) {
                street + "\n" + zipCode.orEmpty() + " " + city
            } else null

    val distanceLabel: String?
        get() = distance?.formatDistance()

    fun updateDistance(to: Location) =
        location.distanceTo(to).also {
            distance = it
        }

    override fun compareTo(other: Item): Int = distance?.compareTo(other.distance ?: 0f) ?: 0
}

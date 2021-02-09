package io.snabble.sdk

data class Brand(
    val id: String,
    val name: String,
) : Comparable<Brand> {
    override fun compareTo(other: Brand) = compareValuesBy(this, other,
        { it.id },
    )
}
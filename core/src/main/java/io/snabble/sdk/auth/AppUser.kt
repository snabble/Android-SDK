package io.snabble.sdk.auth

data class AppUser(
    @JvmField
    val id: String,
    @JvmField
    val secret: String
)
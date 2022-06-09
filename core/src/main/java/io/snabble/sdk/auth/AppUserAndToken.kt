package io.snabble.sdk.auth

data class AppUserAndToken(
    @JvmField
    val token: Token,
    @JvmField
    val appUser: AppUser
)
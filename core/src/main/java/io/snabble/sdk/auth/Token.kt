package io.snabble.sdk.auth

data class Token(
    @JvmField
    val id: String,
    @JvmField
    val token: String,
    @JvmField
    val issuedAt: Long,
    @JvmField
    val expiresAt: Long
)
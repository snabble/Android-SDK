package io.snabble.sdk.wlanmanager.data

internal sealed interface Result {

    val message: String
}

internal data class Success(override val message: String) : Result

internal data class Error(override val message: String) : Result

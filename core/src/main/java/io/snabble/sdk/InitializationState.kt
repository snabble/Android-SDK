package io.snabble.sdk

/**
 * Enum class describing the current initialization state of the SDK
 */
enum class InitializationState {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED,
    ERROR,
}
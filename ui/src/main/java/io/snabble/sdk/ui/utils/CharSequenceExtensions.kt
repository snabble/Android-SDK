package io.snabble.sdk.ui.utils

internal fun <T : CharSequence> T.emptyToNull(): T? = ifEmpty { null }

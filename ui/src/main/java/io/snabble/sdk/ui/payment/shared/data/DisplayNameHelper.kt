package io.snabble.sdk.ui.payment.shared.data

import java.util.Locale

internal val String.displayName: String
    get() = Locale("", this).displayName

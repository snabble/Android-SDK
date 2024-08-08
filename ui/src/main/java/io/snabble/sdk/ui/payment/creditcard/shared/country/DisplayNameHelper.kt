package io.snabble.sdk.ui.payment.creditcard.shared.country

import java.util.Locale

internal val String.displayName: String
    get() = Locale("", this).displayName

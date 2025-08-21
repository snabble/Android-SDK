package io.snabble.sdk.ui.payment.creditcard.shared.country

import io.snabble.sdk.localeOf

internal val String.displayName: String
    get() = localeOf("", this).displayName

package io.snabble.sdk.widgets.snabble.customercard.domain

import io.snabble.sdk.Snabble

internal class HasCustomerCardUseCase(
    private val snabble: Snabble,
) {

    operator fun invoke(): Boolean = snabble.projects.first().customerCardInfo.isNotEmpty()
}

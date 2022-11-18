package io.snabble.sdk.widgets.snabble.customercard.usecases

import io.snabble.sdk.Snabble

internal interface HasCustomerCardUseCase {

    operator fun invoke(): Boolean
}

internal class HasCustomerCardUseCaseImpl(
    private val snabble: Snabble,
) : HasCustomerCardUseCase {

    override operator fun invoke(): Boolean = snabble.projects.firstOrNull()?.customerCardInfo?.isNotEmpty() == true
}

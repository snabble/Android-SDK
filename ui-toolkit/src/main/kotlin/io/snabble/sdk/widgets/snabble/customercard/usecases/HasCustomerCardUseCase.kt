package io.snabble.sdk.widgets.snabble.customercard.usecases

import io.snabble.sdk.Snabble

interface HasCustomerCardUseCase {

    operator fun invoke(): Boolean
}

internal class HasCustomerCardUseCaseImpl(
    private val Snabble: Snabble,
) : HasCustomerCardUseCase {

    override operator fun invoke(): Boolean = Snabble.projects.first().customerCardInfo.isNotEmpty()
}

package io.snabble.sdk.usecases

import io.snabble.sdk.Snabble

internal class GetCustomerCardInfo(
    private val snabble: Snabble,
) {

    operator fun invoke(): Boolean = snabble.projects.first().customerCardInfo.isNotEmpty()
}

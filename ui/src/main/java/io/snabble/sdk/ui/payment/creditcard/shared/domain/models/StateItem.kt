package io.snabble.sdk.ui.payment.creditcard.shared.domain.models

import io.snabble.sdk.ui.payment.creditcard.shared.data.country.StateDto

internal data class StateItem(val displayName: String, val code: String) {

    companion object {

        fun from(stateDto: StateDto) = StateItem(
            displayName = stateDto.displayName,
            code = stateDto.code
        )
    }
}

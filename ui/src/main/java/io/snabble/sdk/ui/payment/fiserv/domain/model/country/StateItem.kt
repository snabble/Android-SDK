package io.snabble.sdk.ui.payment.fiserv.domain.model.country

import io.snabble.sdk.ui.payment.fiserv.data.dto.country.StateDto

internal data class StateItem(val displayName: String, val code: String) {
    companion object {

        fun from(stateDto: StateDto) = StateItem(
            displayName = stateDto.displayName,
            code = stateDto.code
        )
    }
}

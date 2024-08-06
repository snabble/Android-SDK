package com.tegut.tbox.account.details.domain.model.country

import io.snabble.sdk.ui.payment.telecash.data.dto.country.StateDto

data class StateItem(val displayName: String, val code: String) {
    companion object {
        fun from(stateDto: StateDto) = StateItem(
            displayName = stateDto.displayName,
            code = stateDto.code
        )
    }
}

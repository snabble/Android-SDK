package io.snabble.sdk.usecases

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.snabble.sdk.Snabble

// FIXME: Scope should be internal
class GetCustomerCardInfo(
    private val snabble: Snabble
) {

    operator fun invoke(): MutableState<Boolean> {
        // TODO: Evaluate whether to take checkedInProject or like given
        return mutableStateOf(snabble.projects.first().customerCardInfo.isNotEmpty())
    }
}

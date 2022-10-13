package io.snabble.sdk.widgets.snabble.toggle.usecases

import io.snabble.sdk.widgets.snabble.toggle.repository.ToggleRepository

internal interface SetToggleStateUseCase {

    suspend operator fun invoke(isChecked: Boolean)
}

internal class SetToggleStateUseCaseImpl(
    private val prefKey: String,
    private val toggleRepository: ToggleRepository,
) : SetToggleStateUseCase {

    override suspend fun invoke(isChecked: Boolean) {
        toggleRepository.saveToggleState(key = prefKey, isChecked = isChecked)
    }
}

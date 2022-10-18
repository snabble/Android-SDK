package io.snabble.sdk.widgets.snabble.toggle.usecases

import io.snabble.sdk.widgets.snabble.toggle.repository.ToggleRepository

internal interface SaveToggleStateUseCase {

    operator fun invoke(isChecked: Boolean)
}

internal class SaveToggleStateUseCaseImpl(
    private val prefKey: String,
    private val toggleRepository: ToggleRepository,
) : SaveToggleStateUseCase {

    override fun invoke(isChecked: Boolean) {
        toggleRepository.saveToggleState(key = prefKey, isChecked = isChecked)
    }
}

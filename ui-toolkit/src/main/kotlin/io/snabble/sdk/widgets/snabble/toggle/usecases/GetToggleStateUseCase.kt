package io.snabble.sdk.widgets.snabble.toggle.usecases

import io.snabble.sdk.widgets.snabble.toggle.repository.ToggleRepository
import kotlinx.coroutines.flow.Flow

internal interface GetToggleStateUseCase {

    suspend operator fun invoke(): Flow<Boolean>
}

internal class GetToggleStateUseCaseImpl(
    private val prefKey: String,
    private val toggleRepository: ToggleRepository,
) : GetToggleStateUseCase {

    override suspend fun invoke(): Flow<Boolean> = toggleRepository.getToggleState(key = prefKey)
}

package io.snabble.sdk.screens.profile.usecases

import io.snabble.sdk.dynamicview.domain.config.ConfigRepository
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig

interface GetProfileConfigUseCase {

    suspend operator fun invoke(): DynamicConfig
}

internal class GetProfileConfigUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetProfileConfigUseCase {

    override suspend operator fun invoke(): DynamicConfig = configRepository.getConfig(CONFIG_FILE_NAME)

    companion object {

        private const val CONFIG_FILE_NAME = "profileConfig.json"
    }
}

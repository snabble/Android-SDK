package io.snabble.sdk.screens.devsettings.usecases

import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import io.snabble.sdk.dynamicview.domain.repository.ConfigRepository

internal interface GetDevSettingsConfigUseCase {

    suspend operator fun invoke(): DynamicConfig
}

internal class GetDevSettingsConfigUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetDevSettingsConfigUseCase {

    override suspend operator fun invoke(): DynamicConfig = configRepository.getConfig(CONFIG_FILE_NAME)

    companion object {

        private const val CONFIG_FILE_NAME = "devSettingsConfig.json"
    }
}

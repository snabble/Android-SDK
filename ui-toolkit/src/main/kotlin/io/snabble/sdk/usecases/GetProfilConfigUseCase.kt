package io.snabble.sdk.usecases

import io.snabble.sdk.config.ConfigRepository
import io.snabble.sdk.data.DynamicConfigDto
import io.snabble.sdk.domain.ConfigMapper
import io.snabble.sdk.domain.DynamicConfig

class GetProfileConfigUseCase(
    private val configRepository: ConfigRepository,
    private val configMapper: ConfigMapper,
) {

    suspend operator fun invoke(): DynamicConfig {
        val configDto: DynamicConfigDto = configRepository.getConfig(CONFIG_FILE_NAME)
        return configMapper.mapTo(configDto)
    }

    companion object {

        private const val CONFIG_FILE_NAME = "profileConfig.json"
    }
}
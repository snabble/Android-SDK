package io.snabble.sdk.usecases

import io.snabble.sdk.config.ConfigRepository
import io.snabble.sdk.domain.DynamicConfig

internal class GetHomeConfigUseCase(
    private val configRepository: ConfigRepository,
) {

    suspend operator fun invoke(): DynamicConfig = configRepository.getConfig(CONFIG_FILE_NAME)

    companion object {

        private const val CONFIG_FILE_NAME = "homeConfig.json"
    }
}

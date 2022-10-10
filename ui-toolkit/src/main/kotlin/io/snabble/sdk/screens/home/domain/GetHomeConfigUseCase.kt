package io.snabble.sdk.screens.home.domain


import io.snabble.sdk.dynamicview.domain.config.ConfigRepository
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig

internal class GetHomeConfigUseCase(
    private val configRepository: ConfigRepository,
) {

    suspend operator fun invoke(): DynamicConfig = configRepository.getConfig(CONFIG_FILE_NAME)

    companion object {

        private const val CONFIG_FILE_NAME = "homeConfig.json"
    }
}

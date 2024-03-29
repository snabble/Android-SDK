package io.snabble.sdk.screens.home.usecases

import io.snabble.sdk.dynamicview.domain.repository.ConfigRepository
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig

internal interface GetHomeConfigUseCase {

    suspend operator fun invoke(): DynamicConfig
}

internal class GetHomeConfigUseCaseImpl(
    private val configRepository: ConfigRepository,
) : GetHomeConfigUseCase {

    override suspend operator fun invoke(): DynamicConfig = configRepository.getConfig(CONFIG_FILE_NAME)

    companion object {

        private const val CONFIG_FILE_NAME = "homeConfig.json"
    }
}

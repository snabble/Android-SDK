package io.snabble.sdk.usecases

import io.snabble.sdk.config.ConfigRepository
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.domain.ConfigMapper
import io.snabble.sdk.domain.Root

// FIXME: Scope should be internal
class GetHomeConfigUseCase(
    private val configRepository: ConfigRepository,
    private val configMapper: ConfigMapper,
) {

    suspend operator fun invoke(): Root {
        val rootDto: RootDto = configRepository.getConfig(CONFIG_FILE_NAME)
        return configMapper.mapTo(rootDto)
    }

    companion object {

        private const val CONFIG_FILE_NAME = "homeConfig.json"
    }
}

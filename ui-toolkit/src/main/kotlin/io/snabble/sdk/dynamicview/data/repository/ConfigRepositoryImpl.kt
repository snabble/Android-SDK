package io.snabble.sdk.dynamicview.data.repository

import io.snabble.sdk.dynamicview.data.local.ConfigurationLocalDataSource
import io.snabble.sdk.dynamicview.domain.repository.ConfigRepository
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig

internal class ConfigRepositoryImpl(
    private val configurationLocalDataSource: ConfigurationLocalDataSource,
) : ConfigRepository {

    override suspend fun getConfig(jsonFileName: String): DynamicConfig =
        configurationLocalDataSource.getConfig(jsonFileName)
}

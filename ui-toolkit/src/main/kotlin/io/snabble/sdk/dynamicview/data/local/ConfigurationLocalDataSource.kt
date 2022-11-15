package io.snabble.sdk.dynamicview.data.local

import io.snabble.sdk.dynamicview.data.dto.DynamicConfigDto
import io.snabble.sdk.dynamicview.data.dto.mapper.ConfigMapper
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal interface ConfigurationLocalDataSource {

    suspend fun getConfig(jsonFileName: String): DynamicConfig
}

internal class ConfigurationLocalDataSourceImpl(
    private val fileProvider: ConfigFileProvider,
    private val json: Json,
    private val configMapper: ConfigMapper,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ConfigurationLocalDataSource {

    override suspend fun getConfig(jsonFileName: String): DynamicConfig {
        val configJson = getFile(jsonFileName)
        val configDto = parse<DynamicConfigDto>(configJson)
        return configMapper.mapDtoToItems(configDto)
    }

    private suspend fun getFile(jsonFileName: String): String =
        fileProvider.getFile(jsonFileName)

    private suspend inline fun <reified T> parse(json: String): T =
        withContext(defaultDispatcher) {
            this@ConfigurationLocalDataSourceImpl.json.decodeFromString(json)
        }
}

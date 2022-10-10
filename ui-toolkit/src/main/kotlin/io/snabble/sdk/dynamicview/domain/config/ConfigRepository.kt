package io.snabble.sdk.dynamicview.domain.config

import io.snabble.sdk.dynamicview.data.DynamicConfigDto
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal class ConfigRepository(
    private val fileProvider: ConfigFileProvider,
    val json: Json,
    private val configMapper: ConfigMapper,
) {

    suspend inline fun getConfig(jsonFileName: String): DynamicConfig {
        val configJson = getFile(jsonFileName)
        val configDto = parse<DynamicConfigDto>(configJson)
        return configMapper.mapDtoToItems(configDto)
    }

    private suspend fun getFile(jsonFileName: String): String =
        fileProvider.getFile(jsonFileName)

    private suspend inline fun <reified T> parse(json: String): T =
        withContext(Dispatchers.Default) {
            this@ConfigRepository.json.decodeFromString(json)
        }
}

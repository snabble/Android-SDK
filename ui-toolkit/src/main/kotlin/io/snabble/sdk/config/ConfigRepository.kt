package io.snabble.sdk.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal class ConfigRepository(
    private val fileProvider: ConfigFileProvider,
    private val json: Json,
) {

    suspend inline fun <reified T> getConfig(jsonFileName: String): T {
        val json = getFile(jsonFileName)
        return parse(json)
    }

    private suspend fun getFile(jsonFileName: String): String =
        fileProvider.getFile(jsonFileName)

    private suspend inline fun <reified T> parse(json: String): T =
        withContext(Dispatchers.Default) {
            this@ConfigRepository.json.decodeFromString(json)
        }
}
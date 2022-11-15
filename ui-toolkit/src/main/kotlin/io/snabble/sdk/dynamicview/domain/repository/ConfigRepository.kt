package io.snabble.sdk.dynamicview.domain.repository

import io.snabble.sdk.dynamicview.domain.model.DynamicConfig

internal interface ConfigRepository {

    suspend fun getConfig(jsonFileName: String): DynamicConfig
}

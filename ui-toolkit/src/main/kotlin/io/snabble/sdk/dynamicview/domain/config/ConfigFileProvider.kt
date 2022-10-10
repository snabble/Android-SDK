package io.snabble.sdk.dynamicview.domain.config

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ConfigFileProvider {

    suspend fun getFile(fileName: String): String
}

class ConfigFileProviderImpl(
    private val assetManager: AssetManager,
) : ConfigFileProvider {

    override suspend fun getFile(fileName: String): String = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        assetManager.open(fileName).bufferedReader().readText()
    }
}

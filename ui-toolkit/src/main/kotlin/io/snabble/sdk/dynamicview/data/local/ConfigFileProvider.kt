package io.snabble.sdk.dynamicview.data.local

import android.content.res.AssetManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface ConfigFileProvider {

    suspend fun getFile(fileName: String): String
}

internal class ConfigFileProviderImpl(
    private val assetManager: AssetManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ConfigFileProvider {

    override suspend fun getFile(fileName: String): String = withContext(ioDispatcher) {
        @Suppress("BlockingMethodInNonBlockingContext")
        assetManager.open(fileName).bufferedReader().readText()
    }
}

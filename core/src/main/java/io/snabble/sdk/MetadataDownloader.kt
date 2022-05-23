package io.snabble.sdk

import androidx.annotation.RestrictTo
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import io.snabble.sdk.utils.StringDownloader
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.Logger
import java.io.File
import java.lang.Exception

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class MetadataDownloader(
    okHttpClient: OkHttpClient?,
    bundledFileAssetPath: String?)
    : StringDownloader(okHttpClient) {
    var hasData = false
        private set
    var jsonObject: JsonObject? = null
        private set

    init {
        val storageFile = File(Snabble.internalStorageDirectory, "metadata_v2.json")
        if (bundledFileAssetPath != null) {
            setBundledData(Snabble.application, bundledFileAssetPath, storageFile)
        } else {
            setStorageFile(storageFile)
        }
        url = Snabble.metadataUrl
    }

    @Synchronized
    override fun onDownloadFinished(content: String) {
        try {
            updateStorage(content)
            jsonObject = GsonHolder.get().fromJson(content, JsonObject::class.java)
            hasData = true
        } catch (e: Exception) {
            Logger.e(e.message)
        }
    }
}
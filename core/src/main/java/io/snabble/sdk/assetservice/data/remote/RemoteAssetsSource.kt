package io.snabble.sdk.assetservice.data.remote

import com.google.gson.JsonSyntaxException
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.assetservice.data.dto.AssetDto
import io.snabble.sdk.assetservice.data.dto.AssetVariantDto
import io.snabble.sdk.assetservice.data.dto.ManifestDto
import io.snabble.sdk.assetservice.data.dto.VariantDto
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.apache.commons.io.FilenameUtils
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

interface RemoteAssetsSource {

    suspend fun downloadManifestForProject(project: Project): ManifestDto?

    suspend fun downloadAllAssets(project: Project, files: List<AssetVariantDto>): List<AssetDto?>
}

class RemoteAssetsSourceImpl : RemoteAssetsSource {

    override suspend fun downloadManifestForProject(project: Project): ManifestDto? =
        with(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation: Continuation<ManifestDto?> ->
                val assetsUrl = project.assetsUrl ?: return@suspendCancellableCoroutine

                val request = Request.Builder()
                    .cacheControl(CacheControl.Builder().maxAge(30.seconds).build())
                    .url(assetsUrl)
                    .get()
                    .build()

                project.okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Logger.e(e.message)
                        continuation.resume(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            if (!response.isSuccessful) {
                                continuation.resume(null)
                                return
                            }

                            val manifestDto = GsonHolder.get().fromJson(response.body.string(), ManifestDto::class.java)
                            continuation.resume(manifestDto)
                        } catch (e: JsonSyntaxException) {
                            Logger.e("Manifest download failed: ${e.message}")
                            continuation.resume(null)
                        } finally {
                            response.close()
                        }
                    }
                }
                )
            }
        }

    // Todo: filter if the existing assets out
    override suspend fun downloadAllAssets(
        project: Project,
        files: List<AssetVariantDto>
    ) = withContext(Dispatchers.IO) {

        val assetsUrls = files.mapNotNull { asset ->
            val url: String? = asset.variants[VariantDto.MDPI]
            val format = FilenameUtils.getExtension(asset.name)
            when {
                format.isValidFormat() && asset.name.isRootAsset() -> url
                else -> null
            }
        }

        val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

        assetsUrls.map { url ->
            async {
                semaphore.withPermit {
                    loadAsset(project, url)
                }
            }
        }.awaitAll()
    }

    private suspend fun loadAsset(project: Project, url: String): AssetDto? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url(Snabble.absoluteUrl(url))
                .get()
                .build()

            val call = project.okHttpClient.newCall(request)

            // Handle cancellation
            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        continuation.resume(null)
                        return
                    }

                    val bytes = response.body.bytes()

                    val asset = AssetDto(data = bytes)

                    continuation.resume(asset)
                    response.close()
                }
            })
        }
    }

    private fun String.isValidFormat() = VALID_FORMATS.contains(this)

    private fun String.isRootAsset() = !contains("/")

    companion object {

        private val VALID_FORMATS = listOf("svg", "jpg", "webp")
        private const val MAX_CONCURRENT_REQUESTS = 10
    }
}

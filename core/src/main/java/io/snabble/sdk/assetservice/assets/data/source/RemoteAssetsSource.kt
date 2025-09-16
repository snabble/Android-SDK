package io.snabble.sdk.assetservice.assets.data.source

import com.google.gson.JsonSyntaxException
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.assetservice.assets.data.source.dto.AssetDto
import io.snabble.sdk.assetservice.assets.data.source.dto.AssetVariantDto
import io.snabble.sdk.assetservice.assets.data.source.dto.ManifestDto
import io.snabble.sdk.assetservice.assets.data.source.dto.VariantDto
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
import java.security.MessageDigest
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal interface RemoteAssetsSource {

    /**
     * Downloads the manifest containing metadata info for the Assets (e.g. name of the assets and the variant)
     */
    suspend fun downloadManifest(): ManifestDto?

    /**
     * Downloads the assets (e.g the bytes) for each asset variant provided.
     * The asset needs to be a valid format (.svg, .jpg, .webp) and not has to be in the root folder
     */
    suspend fun downloadAllAssets(files: List<AssetVariantDto>): List<AssetDto>
}

internal class RemoteAssetsSourceImpl(
    private val project: Project
) : RemoteAssetsSource {

    override suspend fun downloadManifest(): ManifestDto? =
        with(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation: Continuation<ManifestDto?> ->
                val assetsUrl = project.assetsUrl ?: return@suspendCancellableCoroutine
                Logger.d("Loading to Manifest from: $assetsUrl")

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
                                Logger.d("Loading manifest failed: ${response.code} ${response.body.string()}")
                                continuation.resume(null)
                                return
                            }

                            val manifestDto = GsonHolder.get().fromJson(response.body.string(), ManifestDto::class.java)
                            Logger.d("Loading manifest succeeded: $manifestDto")
                            continuation.resume(manifestDto)
                        } catch (e: JsonSyntaxException) {
                            Logger.e("Manifest parsing failed: ${e.message}")
                            continuation.resume(null)
                        } finally {
                            response.close()
                        }
                    }
                }
                )
            }
        }

    override suspend fun downloadAllAssets(
        files: List<AssetVariantDto>
    ) = withContext(Dispatchers.IO) {

        val assetsUrls = files.mapNotNull { asset ->
            val url: String = asset.variants[VariantDto.MDPI] ?: return@mapNotNull null
            val assetsName: String = asset.name
            val format = FilenameUtils.getExtension(assetsName)
            when {
                format.isValidFormat() && asset.name.isRootAsset() -> assetsName to url
                else -> null
            }
        }

        Logger.d("Filtered valid assets: $assetsUrls")

        val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

        assetsUrls
            .map { (assetName, url) ->
                async {
                    semaphore.withPermit {
                        loadAsset(project, url, assetName)
                    }
                }
            }.awaitAll()
            .filterNotNull()
    }

    private suspend fun loadAsset(project: Project, url: String, assetName: String): AssetDto? =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                Logger.d("Loading asset for $url")
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
                        Logger.e("Loading asset failed: ${e.message}")
                        continuation.resume(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            Logger.e("Loading asset failed: ${response.code} ${response.body.string()}")
                            continuation.resume(null)
                            return
                        }
                        val bytes = response.body.bytes()
                        val asset =
                            AssetDto(data = bytes.inputStream(), hash = url.calculateHash(), name = assetName)
                        Logger.d("Loading assets succeeded: $asset")
                        continuation.resume(asset)
                        response.close()
                    }
                })
            }
        }

    private fun String.isValidFormat() = VALID_FORMATS.contains(this)

    private fun String.isRootAsset() = !contains("/")

    /**
     * Calculate SHA-256 hash for a String
     */
    fun String.calculateHash(): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(this.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Logger.e("Failed to calculate hash for string: ${e.message}", e)
            throw e
        }
    }

    companion object {

        private val VALID_FORMATS = listOf("svg", "jpg", "webp")
        private const val MAX_CONCURRENT_REQUESTS = 10
    }
}

package io.snabble.sdk

import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.Snabble.application
import io.snabble.sdk.Snabble.projects
import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.ReceiptsApi
import io.snabble.sdk.ReceiptsApi.RawReceiptUpdateCallback
import io.snabble.sdk.Receipts.ReceiptInfoCallback
import io.snabble.sdk.ReceiptsApi.ReceiptUpdateCallback
import io.snabble.sdk.Receipts.ReceiptDownloadCallback
import io.snabble.sdk.Snabble
import io.snabble.sdk.Project
import io.snabble.sdk.utils.Dispatch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.Throws

/**
 * Class to download user receipts in pdf format to internal storage.
 */
class Receipts internal constructor() {
    private val receiptsApi = ReceiptsApi()
    private var call: Call? = null

    /**
     * Get the raw api response of the receipts
     */
    fun getRawReceipts(rawReceiptUpdateCallback: RawReceiptUpdateCallback?) {
        receiptsApi.getRaw(rawReceiptUpdateCallback)
    }

    /**
     * Get the receipt info
     */
    fun getReceiptInfo(receiptInfoCallback: ReceiptInfoCallback) {
        receiptsApi.get(object : ReceiptUpdateCallback {
            override fun success(receiptInfos: Array<ReceiptInfo>) {
                Dispatch.mainThread { receiptInfoCallback.success(receiptInfos) }
            }

            override fun failure() {
                Dispatch.mainThread { receiptInfoCallback.failure() }
            }
        })
    }

    @Deprecated(message = "Use getReceiptInfo instead",
                replaceWith = ReplaceWith("getReceiptInfo(receiptInfoCallback)"))
    fun getReceiptInfos(receiptInfoCallback: ReceiptInfoCallback) {
        getReceiptInfo(receiptInfoCallback)
    }

    /**
     * Cancel the currently outstanding download.
     */
    fun cancelDownload() {
        call?.cancel()
        call = null
    }

    /**
     * Downloads a receipts pdf and stores it in the projects internal storage directory
     */
    fun download(receiptInfo: ReceiptInfo, callback: ReceiptDownloadCallback?) {
        if (receiptInfo.pdfUrl == null) {
            callback?.failure()
            return
        }
        val request: Request = Request.Builder()
            .url(receiptInfo.pdfUrl)
            .get()
            .build()

        cancelDownload()

        // .pdf extension is needed for adobe reader to work
        val file = File(instance.application.cacheDir, receiptInfo.id + ".pdf")
        if (file.exists()) {
            callback?.success(file)
            return
        }

        val project = Snabble.projects.find { it.id == receiptInfo.projectId }

        if (project == null) {
            callback?.failure()
            return
        }

        call = project.okHttpClient.newCall(request)
        call?.enqueue(object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val fos = FileOutputStream(file)
                    IOUtils.copy(response.body?.byteStream(), fos)
                    callback?.success(file)
                } else {
                    callback?.failure()
                }
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                callback?.failure()
            }
        })
    }

    interface ReceiptInfoCallback {
        fun success(receiptInfos: Array<ReceiptInfo>?)
        fun failure()
    }

    interface ReceiptDownloadCallback {
        fun success(pdf: File?)
        fun failure()
    }
}

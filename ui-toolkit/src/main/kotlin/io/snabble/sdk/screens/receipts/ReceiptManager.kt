package io.snabble.sdk.screens.receipts

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.Project
import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.Receipts.ReceiptInfoCallback
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkout.Checkout
import io.snabble.sdk.checkout.CheckoutState
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger

object ReceiptManager {

    private const val KEY_IS_LATEST = "isLatest"
    private const val KEY_RECEIPTS = "receipts"

    private val receipts by lazy { Snabble.receipts }
    private val sharedPreferences =
        KoinProvider.getKoin().get<Context>().getSharedPreferences("receipt_manager", Context.MODE_PRIVATE)

    val receiptInfo: MutableLiveData<List<ReceiptInfo>> = MutableLiveData()
    val isLoading = MutableLiveData(false)
    private val handler = Handler(Looper.getMainLooper())

    private var isLatest = false
    private var project: Project? = null

    fun init(project: Project) {
        project.checkout.state.observeForever { state ->
            if (state == CheckoutState.PAYMENT_APPROVED) {
                dispatchUpdate(project, project.checkout)
            }
        }

        load()
    }

    private fun load() {
        isLatest = try {
            val json = sharedPreferences.getString(KEY_RECEIPTS, null)
            if (json != null) {
                val typeToken: TypeToken<List<ReceiptInfo>> = object : TypeToken<List<ReceiptInfo>>() {}
                val newReceiptInfo = GsonHolder.get().fromJson<List<ReceiptInfo>>(json, typeToken.type)
                receiptInfo.postValue(newReceiptInfo)
            } else {
                receiptInfo.postValue(ArrayList())
            }
            sharedPreferences.getBoolean(KEY_IS_LATEST, false)
        } catch (e: Exception) {
            Logger.e(e.message)
            false
        }

        update()
    }

    private fun save() {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val info = receiptInfo.value
            if (info != null) {
                sharedPreferences.edit()
                    .putString(KEY_RECEIPTS, GsonHolder.get().toJson(info))
                    .putBoolean(KEY_IS_LATEST, isLatest)
                    .apply()
            } else {
                sharedPreferences.edit()
                    .putString(KEY_RECEIPTS, null)
                    .putBoolean(KEY_IS_LATEST, isLatest)
                    .apply()
            }
        }
    }

    private fun dispatchUpdate(project: Project?, checkout: Checkout) {
        isLoading.postValue(true)
        sharedPreferences.edit().putBoolean(KEY_IS_LATEST, false).apply()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ update(project, checkout) }, 2000)
    }

    fun update() {
        update(project, project?.checkout)
    }

    private fun update(project: Project?, checkout: Checkout?) {
        isLoading.postValue(true)
        receipts.getReceiptInfo(object : ReceiptInfoCallback {
            override fun success(receiptInfos: Array<ReceiptInfo>?) {
                val receiptInfoList = receiptInfos?.toList() ?: emptyList()

                val isLatest: Boolean = if (dispatchUpdateIfNeeded(receiptInfoList, checkout, project)) {
                    isLoading.postValue(false)
                    true
                } else {
                    false
                }

                receiptInfoList
                    .filterNot { it.pdfUrl == null }
                    .let {
                        set(it, isLatest)
                        receiptInfo.postValue(it)
                    }
                save()
            }

            override fun failure() {
                if (dispatchUpdateIfNeeded(receiptInfo.value, checkout, project)) {
                    isLoading.postValue(false)
                }
            }
        })
    }

    private fun dispatchUpdateIfNeeded(
        receiptInfo: List<ReceiptInfo>?,
        checkout: Checkout?,
        project: Project?,
    ): Boolean {
        if (checkout != null && checkout.state.value == CheckoutState.PAYMENT_APPROVED) {
            var containsOrder = false

            if (checkout.checkoutProcess?.orderId != null && receiptInfo != null) {
                for (info in receiptInfo) {
                    if (info.id == checkout.checkoutProcess?.orderId) {
                        containsOrder = true
                        break
                    }
                }
            } else {
                containsOrder = true
            }

            return if (containsOrder) {
                true
            } else {
                dispatchUpdate(project, checkout)
                false
            }
        }

        return true
    }

    private operator fun set(newReceiptInfos: List<ReceiptInfo>, isLatest: Boolean) {
        ReceiptManager.isLatest = isLatest
        receiptInfo.postValue(newReceiptInfos)
        save()
    }
}

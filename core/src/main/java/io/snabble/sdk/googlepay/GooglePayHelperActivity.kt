package io.snabble.sdk.googlepay

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wallet.AutoResolveHelper
import io.snabble.sdk.Snabble

/**
 * Activity for displaying a google pay overlay
 */
class GooglePayHelperActivity : AppCompatActivity() {
    companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 712
        const val INTENT_EXTRA_PAYMENT_PRICE_TO_PAY = "priceToPay"
        const val INTENT_EXTRA_PROJECT_ID = "projectId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val priceToPay = intent.getStringExtra(INTENT_EXTRA_PAYMENT_PRICE_TO_PAY)
        if (priceToPay != null) {
            googlePayHelper?.loadPaymentData(priceToPay, this, LOAD_PAYMENT_DATA_REQUEST_CODE)
        } else {
            googlePayHelper?.onActivityResult(AutoResolveHelper.RESULT_ERROR, null)
            finish()
        }
    }

    // Deprecation is ignored until an updated integration guide of Google Pay has been published,
    // or another solution can be found:
    // https://developers.google.com/pay/api/android/guides/tutorial#paymentdata
    @Suppress("OVERRIDE_DEPRECATION")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                googlePayHelper?.onActivityResult(resultCode, data)
            }
        }
        finish()
    }

    private val googlePayHelper: GooglePayHelper?
        get() {
            val projectId = intent.getStringExtra(INTENT_EXTRA_PROJECT_ID)
            val project = Snabble.projects.firstOrNull { it.id == projectId }
            if (project != null) {
                return project.googlePayHelper
            }
            return null
        }
}

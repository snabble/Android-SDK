package io.snabble.sdk.googlepay

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.snabble.sdk.Snabble

class GooglePayHelperActivity : AppCompatActivity() {
    companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 712
        const val INTENT_EXTRA_PAYMENT_PRICE_TO_PAY = "priceToPay"
        const val INTENT_EXTRA_PROJECT_ID = "projectId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val projectId = intent.getStringExtra(INTENT_EXTRA_PROJECT_ID)
        val priceToPay = intent.getStringExtra(INTENT_EXTRA_PAYMENT_PRICE_TO_PAY)

        val project = Snabble.getInstance().projects.firstOrNull { it.id == projectId }
        if (project != null && priceToPay != null) {
            val googlePayHelper = project.googlePayHelper
            googlePayHelper.loadPaymentData(priceToPay, this, LOAD_PAYMENT_DATA_REQUEST_CODE)
        } else {
            finish()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                val projectId = intent.getStringExtra(INTENT_EXTRA_PROJECT_ID)
                val project = Snabble.getInstance().projects.firstOrNull { it.id == projectId }
                if (project != null) {
                    val googlePayHelper = project.googlePayHelper
                    googlePayHelper.onActivityResult(resultCode, data)
                }

                finish()
            }
        }
    }

}
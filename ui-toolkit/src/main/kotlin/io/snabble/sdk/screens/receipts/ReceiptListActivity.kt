package io.snabble.sdk.screens.receipts

import android.view.MenuItem
import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

/**
 * Default target for [io.snabble.sdk.SnabbleUiToolkit.Event.SHOW_RECEIPT_LIST] event.
 * Used if no UI action is set for this specific Event.
 * */
class ReceiptListActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment = ReceiptListFragment()

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
}

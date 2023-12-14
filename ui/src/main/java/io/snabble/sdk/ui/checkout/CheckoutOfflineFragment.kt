package io.snabble.sdk.ui.checkout

import android.os.Bundle
import androidx.activity.addCallback
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class CheckoutOfflineFragment : BaseFragment(R.layout.snabble_fragment_checkout_offline) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback {
            Snabble.checkedInProject.value?.checkout?.reset()
        }
    }
}

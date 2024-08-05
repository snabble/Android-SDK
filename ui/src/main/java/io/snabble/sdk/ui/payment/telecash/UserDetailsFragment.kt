package io.snabble.sdk.ui.payment.telecash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

class UserDetailsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setContent {
                UserDetailsScreen(onErrorProcessed = {}, isLoading = false, onSendAction = {}, showError = false)
            }
        }
}

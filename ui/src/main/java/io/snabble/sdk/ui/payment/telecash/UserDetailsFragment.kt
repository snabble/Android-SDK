package io.snabble.sdk.ui.payment.telecash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

class UserDetailsFragment : Fragment() {

    val viewModel: TelecashViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setContent {
                UserDetailsScreen(onErrorProcessed = {}, isLoading = false, onSendAction = {
                    viewModel.preuAuth(it)
                }, showError = false)
            }
        }
}

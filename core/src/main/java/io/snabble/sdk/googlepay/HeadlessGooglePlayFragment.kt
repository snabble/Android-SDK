package io.snabble.sdk.googlepay

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.gms.wallet.contract.TaskResultContracts
import io.snabble.sdk.Snabble

class HeadlessGooglePlayFragment : Fragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        googlePayHelper?.paymentDataLauncher = registerForActivityResult(TaskResultContracts.GetPaymentDataResult()) { result ->
            googlePayHelper?.onResult(result.status, result.result)
        }
    }

    private val googlePayHelper: GooglePayHelper?
        get() {
            val projectId = arguments?.getString(ARG_PROJECT_ID)
            val project = Snabble.projects.firstOrNull { it.id == projectId }
            if (project != null) {
                return project.googlePayHelper
            }
            return null
        }

    companion object {
        const val TAG = "HeadlessGooglePlayFragment"
        const val ARG_PROJECT_ID = "projectId"

        @JvmStatic
        fun newInstance(projectId: String?) = HeadlessGooglePlayFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PROJECT_ID, projectId)
            }
        }
    }
}
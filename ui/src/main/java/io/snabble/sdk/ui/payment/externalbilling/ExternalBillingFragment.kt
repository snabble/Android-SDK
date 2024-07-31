package io.snabble.sdk.ui.payment.externalbilling

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import io.snabble.sdk.config.ProjectId
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class ExternalBillingFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_external_billing,
    waitForProject = false,
) {

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.external_billing_compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            val projectId: ProjectId? = arguments?.getString(ARG_PROJECT_ID)?.let(::ProjectId)

            setContent {
                PaymentExternalBillingScreen(projectId = projectId)
            }
        }
    }

    companion object {

        const val ARG_PROJECT_ID = "projectId"

        fun createFragment(projectId: String?): ExternalBillingFragment =
            ExternalBillingFragment().apply {
                arguments = projectId?.let { bundleOf(ARG_PROJECT_ID to it) }
            }
    }
}

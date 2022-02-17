package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.View
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class ProjectPaymentOptionsFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_select_payment_project,
    waitForProject = false
) {
    companion object {
        const val ARG_BRAND = ProjectPaymentOptionsView.ARG_BRAND
    }

    var brand: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        brand = arguments?.getString(ARG_BRAND)
        if (brand == null) {
            throw IllegalArgumentException("ProjectPaymentOptionsFragment needs a brand id as argument")
        }
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        val v = view as ProjectPaymentOptionsView
        brand?.let { brand ->
            val projects = ArrayList<Project>()
            Snabble.getInstance().projects.forEach { project ->
                if (project.brand?.id == brand) {
                    projects.add(project)
                }
            }
            v.projects = projects
        }
    }
}
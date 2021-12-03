package io.snabble.sdk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.ProjectPaymentOptionsView

open class ProjectPaymentOptionsFragment : Fragment() {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v =  inflater.inflate(
            R.layout.snabble_fragment_select_payment_project,
            container,
            false
        ) as ProjectPaymentOptionsView

        brand?.let { brand ->
            val projects = ArrayList<Project>()
            Snabble.getInstance().projects.forEach { project ->
                if (project.brand?.id == brand) {
                    projects.add(project)
                }
            }
            v.projects = projects
        }

        return v
    }
}
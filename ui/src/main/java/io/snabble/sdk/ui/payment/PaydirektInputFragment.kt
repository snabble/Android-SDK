package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class PaydirektInputFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_paydirekt,
    waitForProject = false
)
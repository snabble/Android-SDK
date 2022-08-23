package io.snabble.sdk.onboarding

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class OnboardingActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment {
        val fragment = OnboardingFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}
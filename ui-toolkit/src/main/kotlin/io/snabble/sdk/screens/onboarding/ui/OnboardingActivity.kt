package io.snabble.sdk.screens.onboarding.ui

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class OnboardingActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment {
        val fragment = OnboardingFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}
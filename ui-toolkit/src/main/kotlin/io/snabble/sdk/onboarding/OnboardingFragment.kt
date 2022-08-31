package io.snabble.sdk.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.SpannedString
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.text.getSpans
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.snabble.accessibility.accessibility
import io.snabble.accessibility.isTalkBackActive
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.onboarding.entities.OnboardingItem
import io.snabble.sdk.onboarding.entities.OnboardingModel
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.resolveImageOrHide
import io.snabble.sdk.utils.LinkClickListener
import io.snabble.sdk.utils.ZoomOutPageTransformer
import io.snabble.sdk.utils.resolveTextOrHide

open class OnboardingFragment : Fragment() {
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v: View = inflater.inflate(R.layout.snabble_fragment_onboarding, container, false)
        val model = arguments?.getParcelable<OnboardingModel>("model") ?: throw IllegalArgumentException()
        val headerImage = v.findViewById<ImageView>(R.id.image_header)
        headerImage.resolveImageOrHide(model.configuration?.imageSource)

        viewPager = v.findViewById(R.id.view_pager)
        viewPager.adapter = StepAdapter(model)
        viewPager.offscreenPageLimit = 1
        (viewPager.getChildAt(0) as? RecyclerView)?.setItemViewCacheSize(0)

        val circleIndicator = v.findViewById<TabLayout>(R.id.circle_indicator)
        TabLayoutMediator(circleIndicator, viewPager) { _, _ -> }.attach()

        val button = v.findViewById<Button>(R.id.button)

        button.setOnClickListener {
            if (viewPager.currentItem < model.items.lastIndex) {
                viewPager.currentItem += 1
            } else {
                SnabbleUiToolkit.executeAction(requireContext(), SnabbleUiToolkit.Event.SHOW_ONBOARDING_DONE)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val index = viewPager.currentItem - 1
                    if (index < 0) {
                        requireActivity().finish()
                    } else {
                        viewPager.setCurrentItem(index, true)
                    }
                }
            })

        // from https://dev.to/bhullnatik/how-to-access-views-directly-with-viewpager2-3bo8
        fun ViewPager2.findViewHolderForAdapterPosition(position: Int) =
            (getChildAt(0) as? RecyclerView)?.findViewHolderForAdapterPosition(position)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            var firstRun = true

            // the OnboardingStepView is only accessible when the scroll state becomes idle
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE && !firstRun && requireContext().isTalkBackActive) {
                    // this is actual a bit hacky way to get the OnboardingStepView, at first we try
                    // to get the internal RecyclerView of the ViewPager2. There we take the
                    // ViewHolder to get the first child which should be OnboardingStepView.
                    // When a view could not be found fallback to the logical parent
                    val viewPagerContent = viewPager.findViewHolderForAdapterPosition(viewPager.currentItem)?.itemView
                    val actualContent = (viewPagerContent as? ViewGroup)?.get(0)
                    val view = actualContent ?: viewPagerContent ?: viewPager

                    // Focus the new OnboardingStepView to read the content
                    view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
                    view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
                }
            }

            override fun onPageSelected(position: Int) {
                val item = model.items[position]
                button.text = item.customButtonTitle ?: getString(
                    if (position == model.items.lastIndex)
                        R.string.Snabble_Onboarding_done
                    else R.string.Snabble_Onboarding_next
                )
                firstRun = false
            }
        })

        // Change the accessibility order of items by code: Logo -> ViewPager -> Button -> Indicator
        ViewCompat.setAccessibilityDelegate(viewPager, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(v: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(v, info)
                info.setTraversalBefore(button)
            }
        })
        ViewCompat.setAccessibilityDelegate(circleIndicator, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(v: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(v, info)
                info.setTraversalAfter(button)
            }
        })

        viewPager.setPageTransformer(ZoomOutPageTransformer())

        return v
    }

    private class StepViewHolder(itemView: OnboardingStepView) : RecyclerView.ViewHolder(itemView) {
        fun bind(data: OnboardingItem) {
            (itemView as OnboardingStepView).bind(data)
        }
    }

    private class StepAdapter(
        val onboardingModel: OnboardingModel
    ) : RecyclerView.Adapter<StepViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder =
            StepViewHolder(OnboardingStepView(parent.context))

        @SuppressLint("ResourceType")
        override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
             holder.bind(onboardingModel.items[position])
        }

        override fun getItemCount() = onboardingModel.items.size
    }
}
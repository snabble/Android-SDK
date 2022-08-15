package io.snabble.sdk.onboarding


import android.content.Context
import android.net.Uri
import android.os.Bundle
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
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.snabble.sdk.onboarding.entities.OnboardingModel
import io.snabble.sdk.ui.isTalkBackActive
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.getImageId
import io.snabble.sdk.ui.utils.getResourceString
import io.snabble.sdk.ui.utils.setImageResourceOrHide
import io.snabble.sdk.ui.utils.setTextOrHide
import io.snabble.sdk.utils.LinkClickListener
import io.snabble.sdk.utils.ZoomOutPageTransformer
import java.lang.IllegalArgumentException

open class OnboardingFragment : Fragment() {


    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v: View = inflater.inflate(R.layout.fragment_onboarding, container, false)

        val model = arguments?.getParcelable<OnboardingModel>("model")?:throw IllegalArgumentException()

        val button = v.findViewById<Button>(R.id.button)
        button.setOnClickListener {
            if (viewPager.currentItem < model.items.lastIndex) {
                viewPager.currentItem += 1
            } else {
//                Settings.seenOnboarding = true
//                findNavController().navigate(OnboardingFragmentDirections.finish())
            }
        }

        viewPager = v.findViewById(R.id.view_pager)
        viewPager.adapter = StepAdapter(requireContext(), LayoutInflater.from(requireContext()),model)

        val circleIndicator = v.findViewById<TabLayout>(R.id.circle_indicator)
        TabLayoutMediator(circleIndicator, viewPager) { _, _ -> }.attach()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
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
        fun ViewPager2.findViewHolderForAdapterPosition(position: Int): RecyclerView.ViewHolder? {
            return (getChildAt(0) as? RecyclerView)?.findViewHolderForAdapterPosition(position)
        }

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
                val resourceId = requireContext().resources.getIdentifier(model.items[position].nextButtonTitle,"string",requireContext().packageName)
                button.text = requireContext().resources.getString(resourceId)
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

    private class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private class StepAdapter(val context: Context,
                              val layoutInflater: LayoutInflater, val onboardingModel: OnboardingModel) : RecyclerView.Adapter<StepViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
            val layout = FrameLayout(context)
            layout.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            return StepViewHolder(layout)
        }

        override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
            val layout = holder.itemView as FrameLayout
            layout.removeAllViews()
            val page = layoutInflater.inflate(R.layout.snabble_view_onboarding_step, layout, true)
            val item = onboardingModel.items[position]

            page.findViewById<ImageView>(R.id.image).setImageResourceOrHide(item.imageSource?.getImageId(context))
            page.findViewById<TextView>(R.id.text).setTextOrHide(item.text?.getResourceString(context))
            page.findViewById<TextView>(R.id.title).setTextOrHide(item.title?.getResourceString(context))
            page.findViewById<TextView>(R.id.footer).setTextOrHide(item.footer?.getResourceString(context))

            if (position == 0 && context.isTalkBackActive) {
                page.findViewById<TextView>(R.id.footer).apply {
                    text = text.toString()
                    movementMethod = null
                    setOnClickListener {
                        findNavController().navigate(
                            NavDeepLinkRequest.Builder.fromUri(
                            Uri.parse("teo://privacy")
                        ).build())
                        //TODO Rollback after plugin fix: findNavController().navigate(LegalFragmentDeeplink.deepLink())
                    }
                    setOnLongClickListener {
                        findNavController().navigate(NavDeepLinkRequest.Builder.fromUri(
                            Uri.parse("teo://terms")
                        ).build())
                        //TODO Rollback after plugin fix: findNavController().navigate(LegalFragmentDeeplink.deepLink())
                        true
                    }

                    ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
                        override fun onInitializeAccessibilityNodeInfo(v: View, info: AccessibilityNodeInfoCompat) {
                            super.onInitializeAccessibilityNodeInfo(v, info)
                            info.addAction(
                                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                    AccessibilityNodeInfoCompat.ACTION_CLICK,
                                    context.getString(R.string.Onboarding_Accessibility_openToC)
                                )
                            )
                            info.addAction(
                                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                    AccessibilityNodeInfoCompat.ACTION_LONG_CLICK,
                                    context.getString(R.string.Onboarding_Accessibility_openPrivacy)
                                )
                            )
                        }
                    })
                }}
             else if (position == 0) {
                page.findViewById<TextView>(R.id.footer).movementMethod = LinkClickListener { url ->
                    val withoutNavigation = url.buildUpon().encodedQuery("hideBottomNavigation=true").build()
                    page.findNavController().navigate(
                        NavDeepLinkRequest.Builder.fromUri(withoutNavigation).build()
                    )
                }
            }
        }

        override fun getItemCount() = onboardingModel.items.size



    }


}
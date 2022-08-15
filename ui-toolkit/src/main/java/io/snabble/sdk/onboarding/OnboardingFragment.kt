package io.snabble.sdk.onboarding


import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
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
import androidx.core.os.bundleOf
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
import com.squareup.picasso.Picasso
import io.snabble.sdk.onboarding.entities.OnboardingModel
import io.snabble.sdk.ui.isTalkBackActive
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.getImageId
import io.snabble.sdk.ui.utils.getResourceId
import io.snabble.sdk.ui.utils.isNotNullOrBlank
import io.snabble.sdk.utils.LinkClickListener
import io.snabble.sdk.utils.ZoomOutPageTransformer
import java.lang.IllegalArgumentException

open class OnboardingFragment : Fragment() {

    companion object {

        const val KEY_SEEN_ONBOARDING = "seen_onboarding"

        fun TextView.resolveTextOrHide(string: String?) {
            if (string.isNotNullOrBlank()) {
                val resId = string!!.getResourceId(context)
                if (resId != Resources.ID_NULL) {
                    setText(resId)
                } else {
                    text = string
                }
                isVisible = true
            } else {
                isVisible = false
            }
        }

        //TODO: Change to ImageView
        fun resolveIntoImageOrTextView(string: String?, imageHybridView: ImageHybridView) {
            imageHybridView.isVisible = false
            val imageView = imageHybridView.findViewById<ImageView>(R.id.image)
            val textView = imageHybridView.findViewById<TextView>(R.id.image_alt_text)
            imageView.isVisible = false
            textView.isVisible = false

            if (string.isNotNullOrBlank()) {
                imageHybridView.isVisible = true
                if (string!!.startsWith("http")) {
                    imageView.isVisible = true
                    Picasso.get().load(string).into(imageView)
                } else {
                    val imageId = string.getImageId(imageView.context)
                    if (imageId != Resources.ID_NULL) {
                        imageView.isVisible = true
                        imageView.setImageResource(imageId)
                    } else {
                        //TODO: Show default image instead of alt text
                        textView.resolveTextOrHide(string)
                    }
                }
            }
        }
    }

    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v: View = inflater.inflate(R.layout.snabble_fragment_onboarding, container, false)
        val model = arguments?.getParcelable<OnboardingModel>("model") ?: throw IllegalArgumentException()
        val config = model.configuration
        val headerImage = v.findViewById<ImageHybridView>(R.id.image_header)

        resolveIntoImageOrTextView(model.configuration.imageSource, headerImage)

        viewPager = v.findViewById(R.id.view_pager)
        viewPager.adapter = StepAdapter(
            requireContext(),
            LayoutInflater.from(requireContext()),
            model
        )

        val circleIndicator = v.findViewById<TabLayout>(R.id.circle_indicator)
        TabLayoutMediator(circleIndicator, viewPager) { _, _ -> }.attach()

        val fullscreenButton = v.findViewById<Button>(R.id.button)
        val prevButton = v.findViewById<Button>(R.id.button_left)
        val nextButton = v.findViewById<Button>(R.id.button_right)

        if (config.hasPageControl == false) {
            viewPager.isUserInputEnabled = false
            circleIndicator.isVisible = false
        }

        nextButton.setOnClickListener {
            if (viewPager.currentItem < model.items.lastIndex) {
                viewPager.currentItem += 1
            } else {
                val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("preferences", Context.MODE_PRIVATE)
                sharedPreferences
                    .edit()
                    .putBoolean(KEY_SEEN_ONBOARDING, true)
                    .apply()
                parentFragmentManager.popBackStack()
                parentFragmentManager.setFragmentResult("onboarding", bundleOf("seen_onboarding" to true))
            }
        }

        prevButton.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        fullscreenButton.setOnClickListener {
            if (viewPager.currentItem < model.items.lastIndex) {
                viewPager.currentItem += 1
            } else {
                val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("preferences", Context.MODE_PRIVATE)
                sharedPreferences
                    .edit()
                    .putBoolean(KEY_SEEN_ONBOARDING, true)
                    .apply()
                //TODO: switch to shared viewmodel
                parentFragmentManager.setFragmentResult("onbaording", bundleOf("seen_onboarding" to true))
                parentFragmentManager.popBackStack()
            }
        }

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
                val item = model.items[position]
                if (item.nextButtonTitle.isNullOrBlank() || item.prevButtonTitle.isNullOrBlank()) {
                    prevButton.isVisible = false
                    nextButton.isVisible = false
                    if (item.nextButtonTitle.isNotNullOrBlank()) {
                        fullscreenButton.resolveTextOrHide(item.nextButtonTitle)
                    } else {
                        fullscreenButton.resolveTextOrHide(item.prevButtonTitle)
                    }
                } else {
                    fullscreenButton.isVisible = false
                    prevButton.resolveTextOrHide(item.prevButtonTitle)
                    nextButton.resolveTextOrHide(item.nextButtonTitle)

                }
                firstRun = false
            }
        })

        // Change the accessibility order of items by code: Logo -> ViewPager -> Button -> Indicator
        ViewCompat.setAccessibilityDelegate(viewPager, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(v: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(v, info)
                info.setTraversalBefore(fullscreenButton)
            }
        })
        ViewCompat.setAccessibilityDelegate(circleIndicator, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(v: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(v, info)
                info.setTraversalAfter(fullscreenButton)
            }
        })

        viewPager.setPageTransformer(ZoomOutPageTransformer())

        return v
    }

    private class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private class StepAdapter(
        val context: Context,
        val layoutInflater: LayoutInflater,
        val onboardingModel: OnboardingModel
    ) : RecyclerView.Adapter<StepViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
            val layout = FrameLayout(context)
            layout.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            return StepViewHolder(layout)
        }

        @SuppressLint("ResourceType")
        override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
            val layout = holder.itemView as FrameLayout
            layout.removeAllViews()
            val page = layoutInflater.inflate(R.layout.snabble_view_onboarding_step, layout, true)
            val item = onboardingModel.items[position]

            val imageHybridView = page.findViewById<ImageHybridView>(R.id.image_hybrid)
            val text = page.findViewById<TextView>(R.id.text)
            val title = page.findViewById<TextView>(R.id.title)
            val footer = page.findViewById<TextView>(R.id.footer)
            val termsButton = page.findViewById<Button>(R.id.terms_button)

            resolveIntoImageOrTextView(item.imageSource, imageHybridView)
            text.resolveTextOrHide(item.text)
            title.resolveTextOrHide(item.title)
            footer.resolveTextOrHide(item.footer)
            termsButton.resolveTextOrHide(item.termsButtonTitle)

            termsButton.setOnClickListener {
                val withoutNavigation = Uri.parse(item.termsLink).buildUpon().encodedQuery("hideBottomNavigation=true").build()
                    page.findNavController().navigate(
                        NavDeepLinkRequest.Builder.fromUri(withoutNavigation).build()
                    )
            }

            if (context.isTalkBackActive){
                if (item.privacyLink.isNotNullOrBlank() || item.termsLink.isNotNullOrBlank()){

                    //Depending on the project both views can hold links. Thus converting both
                    //strings for Talkback
                    text.apply {
                        this.text = text.toString()
                        movementMethod = null
                    }
                    footer.apply {
                        this.text = text.toString()
                        movementMethod = null
                    }

                    //Moving the click and long click feature on the view for easier interaction
                    footer.apply {
                        if (item.termsLink.isNotNullOrBlank()) {
                            setOnClickListener {
                                findNavController().navigate(
                                    NavDeepLinkRequest.Builder.fromUri(
                                        Uri.parse(item.termsLink)
                                    ).build()
                                )
                                //TODO Rollback after plugin fix: findNavController().navigate(LegalFragmentDeeplink.deepLink())
                            }
                        }
                        if (item.privacyLink.isNotNullOrBlank()) {
                            setOnLongClickListener {
                                findNavController().navigate(
                                    NavDeepLinkRequest.Builder.fromUri(
                                        Uri.parse(item.privacyLink)
                                    ).build()
                                )
                                //TODO Rollback after plugin fix: findNavController().navigate(LegalFragmentDeeplink.deepLink())
                                true
                            }
                        }

                        //Short click to open Terms, long click to open privacy if set
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
                    }

                }
            }
            //handle link click inside the App,
            //TODO: Bug fix with bottomNavigationBar, remains white after backpress
            else if (item.privacyLink.isNotNullOrBlank() || item.termsLink.isNotNullOrBlank()) {
                text.movementMethod = LinkClickListener { url ->
                    val withoutNavigation = url.buildUpon().encodedQuery("hideBottomNavigation=true").build()
                    page.findNavController().navigate(
                        NavDeepLinkRequest.Builder.fromUri(withoutNavigation).build()
                    )
                }
                footer.movementMethod = LinkClickListener { url ->
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
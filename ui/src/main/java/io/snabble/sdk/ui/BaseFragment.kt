package io.snabble.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.snabble.sdk.InitializationState
import io.snabble.sdk.Snabble

abstract class BaseFragment : Fragment() {
    private lateinit var sdkNotInitialized: TextView
    private lateinit var fragmentContainer: ViewGroup

    var isReady = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.snabble_fragment_base, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fragmentContainer = view.findViewById(R.id.fragment_container)
        sdkNotInitialized = view.findViewById(R.id.sdk_not_initialized)

        Snabble.initializationState.observe(viewLifecycleOwner) {
            when(it) {
                InitializationState.INITIALIZED -> {
                    waitForProjectAndAdd(savedInstanceState)
                }
                InitializationState.INITIALIZING -> {
                    waitForProjectAndAdd(savedInstanceState)
                }
                InitializationState.ERROR -> {
                    sdkNotInitialized.isVisible = true
                }
            }
        }
    }

    private fun waitForProjectAndAdd(savedInstanceState: Bundle?) {
        sdkNotInitialized.isVisible = false

        SnabbleUI.projectAsLiveData.observe(viewLifecycleOwner) {
            if (it != null) {
                if (fragmentContainer.childCount == 0) {
                    val fragmentView =
                        onCreateActualView(layoutInflater, fragmentContainer, savedInstanceState)
                    if (fragmentView != null) {
                        sdkNotInitialized.isVisible = false
                        fragmentContainer.addView(fragmentView)
                        isReady = true
                    }
                }
            } else {
                isReady = false
                fragmentContainer.removeAllViews()
            }
        }
    }

    abstract fun onCreateActualView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?)
    : View?
}
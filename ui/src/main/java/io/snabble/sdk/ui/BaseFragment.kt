package io.snabble.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.snabble.sdk.InitializationState
import io.snabble.sdk.Snabble

abstract class BaseFragment : Fragment() {
    private lateinit var sdkNotInitialized: TextView
    private lateinit var progress: ProgressBar
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
        progress = view.findViewById(R.id.progress)
        sdkNotInitialized = view.findViewById(R.id.sdk_not_initialized)

        Snabble.getInstance().initializationState.observe(viewLifecycleOwner) {
            when(it) {
                InitializationState.NONE -> {
                    waitForProjectAndAdd(savedInstanceState)
                }
                InitializationState.INITIALIZED -> {
                    waitForProjectAndAdd(savedInstanceState)
                }
                InitializationState.INITIALIZING -> {
                    waitForProjectAndAdd(savedInstanceState)
                }
                InitializationState.ERROR -> {
                    progress.isVisible = false
                    sdkNotInitialized.isVisible = true
                }
            }
        }
    }

    private fun waitForProjectAndAdd(savedInstanceState: Bundle?) {
        sdkNotInitialized.isVisible = false

        SnabbleUI.projectAsLiveData.observe(viewLifecycleOwner) {
            progress.isVisible = it == null

            if (it != null) {
                if (fragmentContainer.childCount == 0) {
                    val fragmentView =
                        onCreateViewInternal(layoutInflater, fragmentContainer, savedInstanceState)
                    if (fragmentView != null) {
                        fragmentContainer.addView(fragmentView)
                        isReady = true
                    }
                }
            } else {
                isReady = false
                fragmentContainer.removeAllViews()
                sdkNotInitialized.isVisible = true
            }
        }
    }

    abstract fun onCreateViewInternal(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?)
    : View?
}
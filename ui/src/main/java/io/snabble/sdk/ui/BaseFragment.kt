package io.snabble.sdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.snabble.sdk.InitializationState
import io.snabble.sdk.Snabble

abstract class BaseFragment(@LayoutRes val layoutResId: Int = 0, val waitForProject: Boolean = true) : Fragment() {
    private lateinit var sdkNotInitialized: TextView
    private lateinit var fragmentContainer: ViewGroup

    var isReady = false

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.snabble_fragment_base, container, false)
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fragmentContainer = view.findViewById(R.id.fragment_container)
        sdkNotInitialized = view.findViewById(R.id.sdk_not_initialized)

        Snabble.initializationState.observe(viewLifecycleOwner) {
            when (it) {
                InitializationState.INITIALIZED -> {
                    waitForProjectAndAdd(savedInstanceState)
                }
                InitializationState.ERROR -> {
                    sdkNotInitialized.isVisible = true
                }
                InitializationState.UNINITIALIZED,
                InitializationState.INITIALIZING,
                null -> Unit // ignore
            }
        }
    }

    private fun waitForProjectAndAdd(savedInstanceState: Bundle?) {
        sdkNotInitialized.isVisible = false

        if (waitForProject) {
            if (Snabble.checkedInProject.value == null) {
                isReady = false
                fragmentContainer.removeAllViews()
                sdkNotInitialized.isVisible = true
            } else {
                Snabble.checkedInProject.observe(viewLifecycleOwner) {
                    if (it != null) {
                        commitView(savedInstanceState)
                    } else {
                        isReady = false
                        fragmentContainer.removeAllViews()
                        sdkNotInitialized.isVisible = true
                    }
                }
            }
        } else {
            commitView(savedInstanceState)
        }
    }

    private fun commitView(savedInstanceState: Bundle?) {
        if (fragmentContainer.childCount == 0) {
            val fragmentView =
                onCreateActualView(layoutInflater,
                    fragmentContainer,
                    savedInstanceState)
            if (fragmentView != null) {
                sdkNotInitialized.isVisible = false
                fragmentContainer.addView(fragmentView)
                isReady = true
                onActualViewCreated(fragmentView, savedInstanceState)
            }
        }
    }

    open fun onCreateActualView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?)
    : View? {
        if (layoutResId != 0) {
            return inflater.inflate(layoutResId, container, false)
        }

        return null
    }

    open fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {

    }
}

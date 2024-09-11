package io.snabble.sdk.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import com.google.android.material.snackbar.Snackbar
import io.snabble.accessibility.focusForAccessibility
import io.snabble.accessibility.isTalkBackActive
import io.snabble.accessibility.setClickDescription
import io.snabble.sdk.Snabble
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.ui.AccessibilityPreferences
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.remotetheme.SnabblePrimaryButton
import io.snabble.sdk.ui.search.SearchHelper
import io.snabble.sdk.ui.utils.setOneShotClickListener
import io.snabble.sdk.utils.Dispatch

open class SelfScanningFragment : BaseFragment(), MenuProvider {

    private var optionsMenu: Menu? = null
    var selfScanningView: SelfScanningView? = null
    protected lateinit var rootView: ViewGroup
    private lateinit var permissionContainer: View
    private lateinit var askForPermission: SnabblePrimaryButton
    private var canAskAgain = false
    private var isStart = false
    var allowShowingHints = false
    val hasSelfScanningView get() = selfScanningView != null

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // Handle Permission granted/rejected
            if (isGranted) {
                createSelfScanningView()
                requireView().announceForAccessibility(getString(R.string.Snabble_Scanner_Accessibility_eventBackInScanner))
                explainScanner()
            } else {
                canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.CAMERA
                )
                showPermissionRationale()
            }
        }

    override fun onCreateActualView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.snabble_fragment_selfscanning, container, false) as ViewGroup
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)
        rootView = view as ViewGroup
        selfScanningView = null
        permissionContainer = rootView.findViewById(R.id.permission_denied_container)
        askForPermission = view.findViewById(R.id.open_settings)

        if (isPermissionGranted) {
            createSelfScanningView()
            var event = getString(R.string.Snabble_Scanner_Accessibility_eventScannerOpened) + " "
            val project = requireNotNull(Snabble.checkedInProject.value)
            if (project.shoppingCart.isEmpty) {
                event += getString(R.string.Snabble_Scanner_Accessibility_hintCartIsEmpty)
            } else {
                with(project) {
                    event += getString(
                        R.string.Snabble_Scanner_Accessibility_hintCartContent,
                        shoppingCart.size(),
                        priceFormatter.format(shoppingCart.totalPrice)
                    )
                }
            }
            view.announceForAccessibility(event)
            explainScanner()
        } else {
            rootView.removeView(selfScanningView)
            selfScanningView = null

            showPermissionRationale()

            view.announceForAccessibility(
                getString(R.string.Snabble_Scanner_Accessibility_eventScannerOpened)
                        + " "
                        + getString(R.string.Snabble_Scanner_Accessibility_hintPermission)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        isStart = true
    }

    override fun onResume() {
        super.onResume()
        if (isReady) {
            if (isPermissionGranted) {
                createSelfScanningView()
            } else {
                rootView.removeView(selfScanningView)
                selfScanningView = null
                if (isAdded && isStart) {
                    activityResultLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    showPermissionRationale()
                }
            }
            isStart = false
        }
    }

    protected open fun onSelfScanningViewCreated(selfScanningView: SelfScanningView) {}

    private fun createSelfScanningView() {
        (activity as? MenuHost)?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        if (selfScanningView == null) {
            selfScanningView = SelfScanningView(context).apply {
                setAllowShowingHints(allowShowingHints)
            }
            rootView.addView(
                selfScanningView, 0, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            onSelfScanningViewCreated(selfScanningView!!)
        }
        permissionContainer.visibility = View.GONE
        canAskAgain = true

        SearchHelper.lastSearch?.let {
            selfScanningView?.lookupAndShowProduct(
                ScannedCode.parse(requireNotNull(Snabble.checkedInProject.value), it)
            )
            SearchHelper.lastSearch = null
        }
    }

    private val isPermissionGranted: Boolean
        get() = (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)

    private fun showPermissionRationale() {
        permissionContainer.visibility = View.VISIBLE
        if (canAskAgain) {
            askForPermission.setText(R.string.Snabble_askForPermission)
            askForPermission.setOneShotClickListener {
                activityResultLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            askForPermission.setText(R.string.Snabble_goToSettings)
            askForPermission.setOneShotClickListener {
                goToSettings()
            }
        }
    }

    fun goToSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun explainScanner() {
        if (requireContext().isTalkBackActive && selfScanningView != null && !AccessibilityPreferences.suppressScannerHint) {
            with(
                Snackbar.make(
                    requireNotNull(selfScanningView),
                    R.string.Snabble_Scanner_firstScan,
                    Snackbar.LENGTH_INDEFINITE
                )
            ) {
                view.fitsSystemWindows = false
                ViewCompat.setOnApplyWindowInsetsListener(view, null)
                setAction(R.string.Snabble_Scanner_Accessibility_actionUnderstood) {
                    AccessibilityPreferences.suppressScannerHint = true
                    dismiss()
                }
                view.findViewById<Button>(com.google.android.material.R.id.snackbar_action)
                    .setClickDescription(R.string.Snabble_Scanner_Accessibility_actionHideHint)
                gravity = Gravity.TOP
                show()
                Dispatch.mainThread({
                    view.focusForAccessibility()
                }, 100)
            }
        }
    }

    private fun updateTorchIcon() {
        val menuItem = optionsMenu?.findItem(R.id.snabble_action_torch)
        if (selfScanningView?.isTorchEnabled == true) {
            menuItem?.icon = ResourcesCompat.getDrawable(resources, R.drawable.snabble_ic_flashlight_on, null)
        } else {
            menuItem?.icon = ResourcesCompat.getDrawable(resources, R.drawable.snabble_ic_flashlight_off, null)
        }
    }

    var Snackbar.gravity: Int?
        get() = when (view.layoutParams) {
            is CoordinatorLayout.LayoutParams -> (view.layoutParams as CoordinatorLayout.LayoutParams).gravity
            is FrameLayout.LayoutParams -> (view.layoutParams as FrameLayout.LayoutParams).gravity
            else -> null
        }
        set(value) {
            when (view.layoutParams) {
                is CoordinatorLayout.LayoutParams -> (view.layoutParams as CoordinatorLayout.LayoutParams).gravity =
                    value ?: Gravity.NO_GRAVITY
                is FrameLayout.LayoutParams -> (view.layoutParams as FrameLayout.LayoutParams).gravity =
                    value ?: Gravity.NO_GRAVITY
            }
        }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.snabble_menu_scanner, menu)
        optionsMenu = menu
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.snabble_action_search -> {
                selfScanningView?.searchWithBarcode()
            }
            R.id.snabble_action_torch -> {
                selfScanningView?.isTorchEnabled = !(selfScanningView?.isTorchEnabled ?: false)
                updateTorchIcon()
            }
        }

        return true
    }

    companion object {

        const val ARG_SHOW_PRODUCT_CODE = "showProductCode"
    }
}

package io.snabble.sdk.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.payment.PayoneInputView
import io.snabble.sdk.ui.utils.setOneShotClickListener

open class SelfScanningFragment : Fragment() {
    companion object {
        const val ARG_SHOW_PRODUCT_CODE = "showProductCode"
    }

    private var optionsMenu: Menu? = null
    var selfScanningView: SelfScanningView? = null
    protected lateinit var rootView: ViewGroup
    private lateinit var permissionContainer: View
    private lateinit var askForPermission: Button
    private var canAskAgain = false
    private var isStart = false
    var allowShowingHints = false
    val hasSelfScanningView
        get() = selfScanningView != null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(false)
        return inflater.inflate(R.layout.snabble_fragment_selfscanning, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view as ViewGroup
        selfScanningView = null
        permissionContainer = rootView.findViewById(R.id.permission_denied_container)
        askForPermission = rootView.findViewById(R.id.open_settings)

        if (isPermissionGranted) {
            createSelfScanningView()
        }
    }

    override fun onStart() {
        super.onStart()
        isStart = true
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionGranted) {
            createSelfScanningView()
        } else {
            rootView.removeView(selfScanningView)
            selfScanningView = null
            if (isAdded && isStart) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
            } else {
                showPermissionRationale()
            }
        }
        isStart = false
    }

    private fun createSelfScanningView() {
        if (selfScanningView == null) {
            selfScanningView = SelfScanningView(context).apply {
                setAllowShowingHints(allowShowingHints)
            }
            rootView.addView(selfScanningView, 0)
            setHasOptionsMenu(true)
        }
        permissionContainer.visibility = View.GONE
        canAskAgain = true
        handleBundleArgs()
    }

    private fun handleBundleArgs() {
        arguments?.let { args ->
            args.getString(ARG_SHOW_PRODUCT_CODE)?.let { scannableCode ->
                selfScanningView?.lookupAndShowProduct(
                    ScannedCode.parse(
                        SnabbleUI.project,
                        scannableCode
                    )
                )
            }
            arguments = null
        }
    }

    val isPermissionGranted: Boolean
        get() = (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createSelfScanningView()
            } else {
                canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    permissions[0]
                )
                showPermissionRationale()
            }
        }
    }

    private fun showPermissionRationale() {
        permissionContainer.visibility = View.VISIBLE
        if (canAskAgain) {
            askForPermission.text = getString(R.string.Snabble_askForPermission)
            askForPermission.setOneShotClickListener {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
            }
        } else {
            askForPermission.text = getString(R.string.Snabble_goToSettings)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.snabble_menu_scanner, menu)
        optionsMenu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
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

    private fun updateTorchIcon() {
        val menuItem = optionsMenu?.findItem(R.id.snabble_action_torch)
        if (selfScanningView?.isTorchEnabled ?: false) {
            menuItem?.icon = ResourcesCompat.getDrawable(resources, R.drawable.snabble_ic_flashlight_on, null)
        } else {
            menuItem?.icon = ResourcesCompat.getDrawable(resources, R.drawable.snabble_ic_flashlight_off, null)
        }
    }
}
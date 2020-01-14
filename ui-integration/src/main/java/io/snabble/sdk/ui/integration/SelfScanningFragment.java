package io.snabble.sdk.ui.integration;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.SelfScanningView;
import io.snabble.sdk.ui.utils.OneShotClickListener;

public class SelfScanningFragment extends Fragment {
    public static final String ARG_SHOW_PRODUCT_CODE = "showProductCode";

    private SelfScanningView selfScanningView;
    private ViewGroup rootView;
    private View permissionContainer;
    private Button askForPermission;
    private boolean canAskAgain = false;
    private boolean isStart;
    private boolean allowShowingHints;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup)inflater.inflate(R.layout.snabble_fragment_selfscanning, container, false);
        permissionContainer = rootView.findViewById(R.id.permission_denied_container);
        askForPermission = rootView.findViewById(R.id.open_settings);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        isStart = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isPermissionGranted()) {
            createSelfScanningView();
        } else {
            rootView.removeView(selfScanningView);
            selfScanningView = null;

            if(isAdded() && isStart) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
            } else {
                showPermissionRationale();
            }
        }

        isStart = false;
    }

    private void createSelfScanningView() {
        selfScanningView = new SelfScanningView(getContext());
        selfScanningView.setAllowShowingHints(allowShowingHints);

        rootView.addView(selfScanningView);
        permissionContainer.setVisibility(View.GONE);
        canAskAgain = true;
        handleBundleArgs();
    }

    private void handleBundleArgs() {
        Bundle args = getArguments();
        if (args != null) {
            String scannableCode = args.getString("showProductCode");
            if (scannableCode != null) {
                selfScanningView.lookupAndShowProduct(ScannedCode.parse(SnabbleUI.getProject(), scannableCode));
            }
            setArguments(null);
        }
    }

    public boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length >= 1) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createSelfScanningView();
            } else {
                canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permissions[0]);
                showPermissionRationale();
            }
        }
    }

    private void showPermissionRationale() {
        permissionContainer.setVisibility(View.VISIBLE);

        if (canAskAgain) {
            askForPermission.setText(getString(R.string.Snabble_askForPermission));
            askForPermission.setOnClickListener(new OneShotClickListener() {
                @Override
                public void click() {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
                }
            });
        } else {
            askForPermission.setText(getString(R.string.Snabble_goToSettings));
            askForPermission.setOnClickListener(new OneShotClickListener() {
                @Override
                public void click() {
                    goToSettings();
                }
            });
        }
    }

    public SelfScanningView getSelfScanningView() {
        return selfScanningView;
    }

    public void setAllowShowingHints(boolean allowShowingHints) {
        this.allowShowingHints = allowShowingHints;
    }

    public void goToSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}

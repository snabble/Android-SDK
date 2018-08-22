package io.snabble.sdk.ui.integration;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import io.snabble.sdk.codes.ScannableCode;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup)inflater.inflate(R.layout.fragment_selfscanning, container, false);
        permissionContainer = rootView.findViewById(R.id.permission_denied_container);
        askForPermission = rootView.findViewById(R.id.open_settings);

        checkPermission();

        return rootView;
    }

    private void checkPermission() {
        if (isPermissionGranted()) {
            createSelfScanningView();
        } else {
            rootView.removeView(selfScanningView);
            selfScanningView = null;

            requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        checkPermission();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isPermissionGranted()) {
            createSelfScanningView();
        } else {
            rootView.removeView(selfScanningView);
            selfScanningView = null;

            showPermissionRationale();
        }
    }

    private void createSelfScanningView() {
        if(selfScanningView == null) {
            selfScanningView = new SelfScanningView(getContext());
            rootView.addView(selfScanningView);
            permissionContainer.setVisibility(View.GONE);

            handleBundleArgs();
        }
    }

    private void handleBundleArgs() {
        Bundle args = getArguments();
        if (args != null) {
            String scannableCode = args.getString("showProductCode");
            if (scannableCode != null) {
                selfScanningView.lookupAndShowProduct(ScannableCode.parse(SnabbleUI.getProject(), scannableCode));
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
        } else {
            canAskAgain = false;
            showPermissionRationale();
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

    public void goToSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}

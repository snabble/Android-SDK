package io.snabble.sdk.ui.integration;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.SelfScanningView;

public class SelfScanningFragment extends Fragment {
    public static final String ARG_SHOW_PRODUCT_CODE = "showProductCode";

    private SelfScanningView selfScanningView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_selfscanning, container, false);

        if (!isPermissionGranted()) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
        }

        selfScanningView = view.findViewById(R.id.self_scanning_view);
        handleBundleArgs();

        return view;
    }

    private void handleBundleArgs() {
        Bundle args = getArguments();
        if (args != null) {
            String scannableCode = args.getString("showProductCode");
            if (scannableCode != null) {
                selfScanningView.lookupAndShowProduct(ScannableCode.parse(SnabbleUI.getSdkInstance(), scannableCode));
            }
            setArguments(null);
        }
    }

    public boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public SelfScanningView getSelfScanningView() {
        return selfScanningView;
    }
}

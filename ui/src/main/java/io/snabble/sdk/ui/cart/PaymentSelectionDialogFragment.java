package io.snabble.sdk.ui.cart;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.payment.PaymentInputViewHelper;

public class PaymentSelectionDialogFragment extends BottomSheetDialogFragment {
    public static final String ARG_ENTRIES = "entries";
    public static final String ARG_SHOW_OFFLINE_HINT = "showOfflineHint";
    public static final String ARG_SELECTED_ENTRY = "selectedEntry";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.snabble_dialog_payment_selection, null);
        LinearLayout options = view.findViewById(R.id.options);

        Bundle args = getArguments();
        if (args != null) {
            View headerView = View.inflate(requireContext(), R.layout.snabble_item_payment_select_header, null);
            options.addView(headerView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            if (args.getBoolean(ARG_SHOW_OFFLINE_HINT, false)) {
                View v = View.inflate(requireContext(), R.layout.snabble_item_payment_select_offline_hint, null);
                options.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            if (args.containsKey(ARG_ENTRIES)) {
                PaymentSelectionHelper.Entry selectedEntry = (PaymentSelectionHelper.Entry) args.getSerializable(ARG_SELECTED_ENTRY);
                ArrayList<PaymentSelectionHelper.Entry> entries = (ArrayList<PaymentSelectionHelper.Entry>) args.getSerializable(ARG_ENTRIES);
                if (entries != null) {
                    boolean hasAnyAddedMethods = false;
                    for (final PaymentSelectionHelper.Entry entry : entries) {
                        if (entry.isAdded) {
                            hasAnyAddedMethods = true;
                            break;
                        }
                    }

                    for (final PaymentSelectionHelper.Entry entry : entries) {
                        View v = View.inflate(requireContext(), R.layout.snabble_item_payment_select, null);

                        ImageView imageView = v.findViewById(R.id.image);
                        TextView name = v.findViewById(R.id.name);
                        TextView id = v.findViewById(R.id.id);
                        View check = v.findViewById(R.id.check);

                        int resId = entry.iconResId;
                        if (resId != 0) {
                            imageView.setImageResource(entry.iconResId);
                        } else {
                            imageView.setVisibility(View.INVISIBLE);
                        }

                        if (entry.isAdded || !hasAnyAddedMethods) {
                            imageView.setColorFilter(null);
                        } else {
                            ColorMatrix matrix = new ColorMatrix();
                            matrix.setSaturation(0);
                            ColorMatrixColorFilter cf = new ColorMatrixColorFilter(matrix);
                            imageView.setColorFilter(cf);
                        }

                        if (entry.text != null) {
                            name.setText(entry.text);
                        } else {
                            name.setVisibility(View.GONE);
                        }

                        if (entry.hint != null) {
                            id.setText(entry.hint);
                        } else {
                            id.setVisibility(View.GONE);
                        }

                        if (entry.isAvailable) {
                            v.setOnClickListener(v1 -> {
                                if (entry.isAdded) {
                                    PaymentSelectionHelper.getInstance().select(entry);
                                    dismissAllowingStateLoss();
                                } else {
                                    PaymentInputViewHelper.openPaymentInputView(requireContext(), entry.paymentMethod, SnabbleUI.getProject().getId());
                                    dismissAllowingStateLoss();
                                }
                            });
                            name.setEnabled(true);
                        } else {
                            name.setEnabled(false);
                        }

                        if (entry == selectedEntry) {
                            check.setVisibility(View.VISIBLE);
                        } else {
                            check.setVisibility(View.GONE);
                        }

                        options.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    }
                }
            }
        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissAllowingStateLoss();
    }
}


package io.snabble.sdk.ui.payment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.OneShotClickListener;

public class SelectPaymentMethodFragment extends BottomSheetDialogFragment {
    public static final String ARG_PROJECT_ID = "projectId";

    private List<Entry> entries;
    private Set<PaymentMethod> paymentMethods;
    private String projectId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            projectId = args.getString(ARG_PROJECT_ID, null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.snabble_view_payment_credentials_select, container, false);

        entries = new ArrayList<>();

        Set<PaymentMethod> availablePaymentMethods = new HashSet<>();
        for (Project project : Snabble.getInstance().getProjects()) {
            if (project.getId().equals(projectId)) {
                availablePaymentMethods.addAll(project.getAvailablePaymentMethods());
            }
        }

        if (paymentMethods == null) {
            paymentMethods = availablePaymentMethods;
        }

        availablePaymentMethods.retainAll(paymentMethods);

        if (availablePaymentMethods.contains(PaymentMethod.DE_DIRECT_DEBIT)) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_payment_select_sepa,
                    "SEPA", getUsableAtText(PaymentMethod.DE_DIRECT_DEBIT), new OneShotClickListener() {
                @Override
                public void click() {
                    PaymentInputViewHelper.openPaymentInputView(requireContext(), PaymentMethod.DE_DIRECT_DEBIT, projectId);
                    dismissAllowingStateLoss();
                }
            }));
        }

        if (availablePaymentMethods.contains(PaymentMethod.PAYONE_SEPA)) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_payment_select_sepa,
                    "SEPA", getUsableAtText(PaymentMethod.PAYONE_SEPA), new OneShotClickListener() {
                @Override
                public void click() {
                    PaymentInputViewHelper.openPaymentInputView(requireContext(), PaymentMethod.PAYONE_SEPA, projectId);
                    dismissAllowingStateLoss();
                }
            }));
        }

        if (availablePaymentMethods.contains(PaymentMethod.VISA)) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_payment_select_visa,
                    "VISA",
                    getUsableAtText(PaymentMethod.VISA), new OneShotClickListener() {
                @Override
                public void click() {
                    PaymentInputViewHelper.openPaymentInputView(requireContext(), PaymentMethod.VISA, projectId);
                    dismissAllowingStateLoss();
                }
            }));
        }

        if (availablePaymentMethods.contains(PaymentMethod.MASTERCARD)) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_payment_select_mastercard,
                    "Mastercard",
                    getUsableAtText(PaymentMethod.MASTERCARD), new OneShotClickListener() {
                @Override
                public void click() {
                    PaymentInputViewHelper.openPaymentInputView(requireContext(), PaymentMethod.MASTERCARD, projectId);
                    dismissAllowingStateLoss();
                }
            }));
        }

        if (availablePaymentMethods.contains(PaymentMethod.AMEX)) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_payment_select_amex,
                    "American Express",
                    getUsableAtText(PaymentMethod.AMEX), new OneShotClickListener() {
                @Override
                public void click() {
                    PaymentInputViewHelper.openPaymentInputView(requireContext(), PaymentMethod.AMEX, projectId);
                    dismissAllowingStateLoss();
                }
            }));
        }

        if (availablePaymentMethods.contains(PaymentMethod.PAYDIREKT)) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_payment_select_paydirekt,
                    "Paydirekt",
                    getUsableAtText(PaymentMethod.PAYDIREKT), new OneShotClickListener() {
                @Override
                public void click() {
                    PaymentInputViewHelper.openPaymentInputView(requireContext(), PaymentMethod.PAYDIREKT, null);
                    dismissAllowingStateLoss();
                }
            }));
        }

        if (availablePaymentMethods.contains(PaymentMethod.TWINT)) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_payment_select_twint,
                    "TWINT",
                    getUsableAtText(PaymentMethod.TWINT), new OneShotClickListener() {
                @Override
                public void click() {
                    PaymentInputViewHelper.openPaymentInputView(requireContext(), PaymentMethod.TWINT, projectId);
                    dismissAllowingStateLoss();
                }
            }));
        }

        if (availablePaymentMethods.contains(PaymentMethod.POST_FINANCE_CARD)) {
            entries.add(new SelectPaymentMethodFragment.Entry(R.drawable.snabble_ic_payment_select_postfinance,
                    "PostFinance Card",
                    getUsableAtText(PaymentMethod.TWINT), new OneShotClickListener() {
                @Override
                public void click() {
                    PaymentInputViewHelper.openPaymentInputView(requireContext(), PaymentMethod.POST_FINANCE_CARD, projectId);
                    dismissAllowingStateLoss();
                }
            }));
        }

        RecyclerView recyclerView = v.findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new SelectPaymentMethodFragment.Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        return v;
    }

    private String getUsableAtText(PaymentMethod... paymentMethods) {
        List<Project> projects = Snabble.getInstance().getProjects();
        if (projects.size() == 1) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        int count = 0;
        for (Project project : projects) {
            List<PaymentMethod> availablePaymentMethods = project.getAvailablePaymentMethods();
            for (PaymentMethod pm : paymentMethods) {
                if (availablePaymentMethods.contains(pm)) {
                    if (count > 0) {
                        sb.append(", ");
                    }

                    sb.append(project.getName());
                    count++;
                    break;
                }
            }
        }

        if (sb.length() == 0) {
            sb.append("TEST");
        }

        return getResources().getString(R.string.Snabble_Payment_usableAt, sb.toString());
    }

    private static class Entry {
        int drawableRes;
        String text;
        String usableAt;
        View.OnClickListener onClickListener;

        Entry(int drawableRes, String text, String usableAt, View.OnClickListener onClickListener) {
            this.drawableRes = drawableRes;
            this.text = text;
            this.usableAt = usableAt;
            this.onClickListener = onClickListener;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.helper_image);
        }
    }

    private class Adapter extends RecyclerView.Adapter<SelectPaymentMethodFragment.ViewHolder> {
        @NonNull
        @Override
        public SelectPaymentMethodFragment.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.snabble_item_payment_credentials_select, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final SelectPaymentMethodFragment.ViewHolder holder, final int position) {
            SelectPaymentMethodFragment.Entry e = entries.get(position);

            if (e.drawableRes != 0) {
                holder.image.setImageResource(e.drawableRes);
            }

            holder.text.setText(e.text);
            holder.itemView.setOnClickListener(e.onClickListener);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }
}

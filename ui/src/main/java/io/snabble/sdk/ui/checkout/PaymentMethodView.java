package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.content.res.Resources;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.ui.PriceFormatter;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;

class PaymentMethodView extends FrameLayout {
    private static Map<PaymentMethod, Integer> icons = new HashMap<>();
    private static Map<PaymentMethod, String> descriptions = new HashMap<>();

    static {
        icons.put(PaymentMethod.CASH, R.drawable.ic_pm_sepa);
        icons.put(PaymentMethod.QRCODE_POS, R.drawable.ic_pm_checkstand);
        icons.put(PaymentMethod.ENCODED_CODES, R.drawable.ic_pm_checkstand);
    }

    private Checkout checkout;
    private List<PaymentMethod> availablePaymentMethods;
    private RecyclerView recyclerView;

    public PaymentMethodView(Context context) {
        super(context);
        inflateView();
    }

    public PaymentMethodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public PaymentMethodView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_payment_method, this);

        Resources res = getResources();
        descriptions.clear();
        //descriptions.put(PaymentMethod.ENCODED_CODES, res.getString(R.string.Snabble_PaymentMethod_encodedCodes));
        //descriptions.put(PaymentMethod.QRCODE_POS, res.getString(R.string.Snabble_PaymentMethod_qrCodePOS));

        Project project = SnabbleUI.getProject();
        checkout = project.getCheckout();
        availablePaymentMethods = new ArrayList<>();

        for (PaymentMethod paymentMethod : checkout.getAvailablePaymentMethods()) {
            if (icons.containsKey(paymentMethod) || descriptions.containsKey(paymentMethod)) {
                availablePaymentMethods.add(paymentMethod);
            }
        }

        recyclerView = findViewById(R.id.payment_methods);
        recyclerView.setAdapter(new Adapter());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(null);

        TextView title = findViewById(R.id.choose_payment_title);

        PriceFormatter priceFormatter = new PriceFormatter(project);
        String totalPriceText = priceFormatter.format(project.getCheckout().getPriceToPay());
        title.setText(getResources().getString(R.string.Snabble_PaymentSelection_howToPay, totalPriceText));
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.image);
        }
    }

    private class Adapter extends RecyclerView.Adapter<PaymentMethodView.ViewHolder> {
        @Override
        public PaymentMethodView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item_payment_method, parent, false);
            return new PaymentMethodView.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final PaymentMethodView.ViewHolder holder, final int position) {
            final PaymentMethod paymentMethod = availablePaymentMethods.get(position);

            Integer drawableResId = icons.get(paymentMethod);
            if (drawableResId != null) {
                ImageView imageView = holder.image;
                imageView.setImageResource(drawableResId);
                imageView.setVisibility(View.VISIBLE);
            }

            String text = descriptions.get(paymentMethod);
            if (text != null) {
                TextView textView = holder.text;
                textView.setText(descriptions.get(paymentMethod));
                textView.setVisibility(View.VISIBLE);
            }

            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkout.pay(paymentMethod);
                    Telemetry.event(Telemetry.Event.SelectedPaymentMethod, paymentMethod);
                }
            });
        }

        @Override
        public int getItemCount() {
            return availablePaymentMethods.size();
        }
    }

}

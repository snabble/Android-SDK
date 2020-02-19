package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;

public class PaymentSuccessView extends FrameLayout {
    public PaymentSuccessView(Context context) {
        super(context);
        inflateView();
    }

    public PaymentSuccessView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public PaymentSuccessView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_payment_success, this);

        if (SnabbleUI.getActionBar() != null) {
            SnabbleUI.getActionBar().setTitle(R.string.Snabble_Checkout_done);
        }

        findViewById(R.id.back).setOnClickListener(v -> {
            SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.execute(SnabbleUI.Action.GO_BACK, null);
            }
        });
    }
}

package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;

public class PaymentFailureView extends FrameLayout {
    public PaymentFailureView(Context context) {
        super(context);
        inflateView();
    }

    public PaymentFailureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public PaymentFailureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_payment_failure, this);

        if (SnabbleUI.getActionBar() != null) {
            SnabbleUI.getActionBar().setTitle(R.string.Snabble_Checkout_error);
        }

        findViewById(R.id.goto_home).setOnClickListener(v -> {
            SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.execute(SnabbleUI.Action.GO_BACK, null);
            }
        });
    }
}

package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;

class CheckoutAbortedView extends FrameLayout {
    public CheckoutAbortedView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutAbortedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutAbortedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_checkout_aborted, this);

        if (SnabbleUI.getActionBar() != null) {
            SnabbleUI.getActionBar().setTitle(R.string.Snabble_Checkout_error);
        }

        findViewById(R.id.goto_home).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.goBack();
                }
            }
        });
    }
}

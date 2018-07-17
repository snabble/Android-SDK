package io.snabble.sdk.ui.payment;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ViewAnimator;
import io.snabble.sdk.ui.R;

public class UserPaymentMethodView extends FrameLayout {
    private ViewAnimator viewAnimator;

    public UserPaymentMethodView(@NonNull Context context) {
        super(context);
        inflateView();
    }

    public UserPaymentMethodView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public UserPaymentMethodView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_userpaymentmethod, this);
        viewAnimator = findViewById(R.id.view_animator);
    }

    public void displayView(View view) {
        viewAnimator.removeAllViews();
        viewAnimator.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }
}

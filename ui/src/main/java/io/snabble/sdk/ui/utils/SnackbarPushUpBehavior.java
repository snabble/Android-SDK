package io.snabble.sdk.ui.utils;

import android.content.Context;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import android.util.AttributeSet;
import android.view.View;

public class SnackbarPushUpBehavior extends CoordinatorLayout.Behavior<View> {
    public SnackbarPushUpBehavior() {

    }

    public SnackbarPushUpBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent,
                                   View child,
                                   View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent,
                                       View child,
                                       View dependency) {
        child.setTranslationY(0);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent,
                                          View child,
                                          View dependency) {
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        child.setTranslationY(translationY);
        return true;
    }
}

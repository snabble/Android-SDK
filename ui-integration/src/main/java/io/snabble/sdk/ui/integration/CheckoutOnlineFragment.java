package io.snabble.sdk.ui.integration;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import io.snabble.sdk.ui.checkout.CheckoutOnlineView;

public class CheckoutOnlineFragment extends Fragment {
    private CheckoutOnlineView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = (CheckoutOnlineView) inflater.inflate(R.layout.snabble_fragment_checkout_online, container, false);
        return view;
    }

    public void setHelperImage(Drawable drawable) {
        view.setHelperImage(drawable);
    }

    public void setHelperImage(@DrawableRes int drawableRes) {
        view.setHelperImage(drawableRes);
    }

    public void setHelperImage(Bitmap bitmap) {
        view.setHelperImage(bitmap);
    }
}

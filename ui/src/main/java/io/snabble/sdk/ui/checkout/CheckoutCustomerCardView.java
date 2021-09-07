package io.snabble.sdk.ui.checkout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.Checkout;
import io.snabble.sdk.Project;
import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.encodedcodes.EncodedCodesGenerator;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.I18nUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.utils.Logger;
import me.relex.circleindicator.CircleIndicator3;

public class CheckoutCustomerCardView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private Project project;
    private Button paidButton;
    private Checkout checkout;
    private Checkout.State currentState;
    private View helperText;
    private ImageView helperImage;
    private View upArrow;
    private BarcodeView barcodeView;

    public CheckoutCustomerCardView(Context context) {
        super(context);
        init();
    }

    public CheckoutCustomerCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckoutCustomerCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        project = SnabbleUI.getProject();
        checkout = SnabbleUI.getProject().getCheckout();

        inflate(getContext(), R.layout.snabble_view_checkout_customercard, this);

        paidButton = findViewById(R.id.paid);
        paidButton.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                project.getCheckout().approveOfflineMethod();
                Telemetry.event(Telemetry.Event.CheckoutFinishByUser);
            }
        });
        paidButton.setText(I18nUtils.getIdentifierForProject(getResources(),
                SnabbleUI.getProject(), R.string.Snabble_QRCode_didPay));

        helperText = findViewById(R.id.helper_text);
        helperImage = findViewById(R.id.helper_image);
        upArrow = findViewById(R.id.arrow);
        barcodeView = findViewById(R.id.barcode_view);

        project.getAssets().get("checkout-offline", this::setHelperImage);

        if (EAN13.isEan13(project.getCustomerCardId())) {
            barcodeView.setFormat(BarcodeFormat.EAN_13);
        } else {
            barcodeView.setFormat(BarcodeFormat.CODE_128);
        }

        barcodeView.setText(project.getCustomerCardId());
        onStateChanged(checkout.getState());
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int dpHeight = Math.round(getHeight() / dm.density);
            if (dpHeight < 500) {
                if (helperImage.getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    helperImage.setLayoutParams(new LinearLayout.LayoutParams(
                            Math.round(helperImage.getWidth() * 0.5f),
                            Math.round(helperImage.getHeight() * 0.5f)));
                }
            } else {
                helperImage.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
    }

    public void setHelperImage(Bitmap bitmap) {
        if (bitmap != null) {
            helperImage.setImageBitmap(bitmap);
            upArrow.setVisibility(View.VISIBLE);
            helperImage.setVisibility(View.VISIBLE);
            helperText.setVisibility(View.GONE);
        } else {
            upArrow.setVisibility(View.GONE);
            helperImage.setVisibility(View.GONE);
            helperText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == currentState) {
            return;
        }

        switch (state) {
            case PAYMENT_APPROVED:
                if (currentState == Checkout.State.PAYMENT_APPROVED) {
                    break;
                }
                Telemetry.event(Telemetry.Event.CheckoutSuccessful);
                SnabbleUI.executeAction(SnabbleUI.Action.SHOW_PAYMENT_STATUS);
                break;
            case PAYMENT_ABORTED:
                Telemetry.event(Telemetry.Event.CheckoutAbortByUser);
                SnabbleUI.executeAction(SnabbleUI.Action.GO_BACK);
                break;
            case DENIED_BY_PAYMENT_PROVIDER:
                Telemetry.event(Telemetry.Event.CheckoutDeniedByPaymentProvider);
                SnabbleUI.executeAction(SnabbleUI.Action.SHOW_PAYMENT_STATUS);
                break;
            case DENIED_BY_SUPERVISOR:
                Telemetry.event(Telemetry.Event.CheckoutDeniedBySupervisor);
                SnabbleUI.executeAction(SnabbleUI.Action.SHOW_PAYMENT_STATUS);
                break;
            case PAYMENT_PROCESSING:
            case PAYMENT_PROCESSING_ERROR:
            case DENIED_TOO_YOUNG:
                SnabbleUI.executeAction(SnabbleUI.Action.SHOW_PAYMENT_STATUS);
                break;
        }

        currentState = state;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (checkout != null) {
            checkout.addOnCheckoutStateChangedListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (checkout != null) {
            checkout.removeOnCheckoutStateChangedListener(this);
        }
    }
}

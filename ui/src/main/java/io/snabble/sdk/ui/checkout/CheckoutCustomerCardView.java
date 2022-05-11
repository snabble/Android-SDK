package io.snabble.sdk.ui.checkout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.checkout.Checkout;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.checkout.CheckoutState;
import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.I18nUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.ViewExtKt;

public class CheckoutCustomerCardView extends FrameLayout {
    private Project project;
    private View helperText;
    private ImageView helperImage;
    private View upArrow;

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
        inflate(getContext(), R.layout.snabble_view_checkout_customercard, this);

        project = Snabble.getInstance().getCheckedInProject().getValue();

        ViewExtKt.observeView(Snabble.getInstance().getCheckedInProject(), this, p -> {
            project = p;
            update();
        });
    }

    private void update() {
        Button paidButton = findViewById(R.id.paid);
        paidButton.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                project.getCheckout().approveOfflineMethod();
                Telemetry.event(Telemetry.Event.CheckoutFinishByUser);
            }
        });
        paidButton.setText(I18nUtils.getIdentifierForProject(getResources(),
                project, R.string.Snabble_QRCode_didPay));

        helperText = findViewById(R.id.helper_text);
        helperImage = findViewById(R.id.helper_image);
        upArrow = findViewById(R.id.arrow);
        BarcodeView barcodeView = findViewById(R.id.barcode_view);

        project.getAssets().get("checkout-offline", this::setHelperImage);

        if (EAN13.isEan13(project.getCustomerCardId())) {
            barcodeView.setFormat(BarcodeFormat.EAN_13);
        } else {
            barcodeView.setFormat(BarcodeFormat.CODE_128);
        }

        barcodeView.setText(project.getCustomerCardId());
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
}
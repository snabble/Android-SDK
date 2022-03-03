package io.snabble.sdk.ui.checkout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.Checkout;
import io.snabble.sdk.Project;
import io.snabble.sdk.encodedcodes.EncodedCodesGenerator;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.scanner.BarcodeView;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.I18nUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.utils.Dispatch;
import me.relex.circleindicator.CircleIndicator3;

public class CheckoutOfflineView extends FrameLayout {
    private Project project;
    private EncodedCodesGenerator encodedCodesGenerator;
    private int maxSizeMm;
    private Button paidButton;
    private ViewPager2 viewPager;
    private ImageView helperImage;
    private View upArrow;
    private CodeListViewAdapter viewPagerAdapter;
    private boolean isSmallDevice;
    private View viewPagerContainer;

    public CheckoutOfflineView(Context context) {
        super(context);
        init();
    }

    public CheckoutOfflineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckoutOfflineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        project = SnabbleUI.getProject();
        EncodedCodesOptions options = project.getEncodedCodesOptions();

        inflate(getContext(), R.layout.snabble_view_checkout_offline, this);

        maxSizeMm = options.maxSizeMm;

        encodedCodesGenerator = new EncodedCodesGenerator(options);

        paidButton = findViewById(R.id.paid);
        paidButton.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                if (viewPager.getCurrentItem() == viewPagerAdapter.getItemCount() - 1) {
                    Project project = SnabbleUI.getProject();
                    project.getCheckout().approveOfflineMethod();
                    Telemetry.event(Telemetry.Event.CheckoutFinishByUser);
                } else {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }
            }
        });
        paidButton.setText(I18nUtils.getIdentifierForProject(getResources(),
                SnabbleUI.getProject(), R.string.Snabble_QRCode_didPay));

        paidButton.setVisibility(View.INVISIBLE);
        paidButton.setAlpha(0);

        Dispatch.mainThread(() -> {
            paidButton.setVisibility(View.VISIBLE);
            paidButton.animate()
                    .setDuration(150)
                    .alpha(1)
                    .start();
            paidButton.setEnabled(true);
        }, 2000);

        helperImage = findViewById(R.id.helper_image);
        upArrow = findViewById(R.id.arrow);

        viewPager = findViewById(R.id.view_pager);
        viewPagerContainer = findViewById(R.id.view_pager_container);

        viewPagerAdapter = new CodeListViewAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == viewPagerAdapter.getItemCount() - 1) {
                    paidButton.setText(I18nUtils.getIdentifierForProject(getResources(),
                            SnabbleUI.getProject(), R.string.Snabble_QRCode_didPay));
                } else {
                    paidButton.setText(getResources().getString(R.string.Snabble_QRCode_nextCode,
                            position + 2,
                            viewPagerAdapter.getItemCount()));
                }
            }
        });

        CircleIndicator3 viewPagerIndicator = findViewById(R.id.view_pager_indicator);
        viewPagerIndicator.setViewPager(viewPager);

        if (viewPagerAdapter.getItemCount() == 1) {
            viewPagerIndicator.setVisibility(View.GONE);
            if (viewPager.getChildCount() > 0) {
                View child = viewPager.getChildAt(0);
                if (child instanceof RecyclerView) {
                    child.setOverScrollMode(OVER_SCROLL_NEVER);
                }
            }
        }

        TextView checkoutId = findViewById(R.id.checkout_id);
        String id = SnabbleUI.getProject().getCheckout().getId();
        if (id != null && id.length() >= 4) {
            checkoutId.setText(id.substring(id.length() - 4));
        } else {
            checkoutId.setVisibility(View.GONE);
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int dpHeight = Math.round(getHeight() / dm.density);
            if (dpHeight < 500) {
                Dispatch.mainThread(() -> {
                    isSmallDevice = true;
                    upArrow.setVisibility(View.GONE);
                    helperImage.setVisibility(View.GONE);
                });
            } else {
                Dispatch.mainThread(() -> {
                    isSmallDevice = false;
                    project.getAssets().get("checkout-offline", this::setHelperImage);
                });
            }

            viewPagerAdapter.notifyDataSetChanged();
        }
    }

    public void setHelperImage(Bitmap bitmap) {
        if (bitmap != null && !isSmallDevice) {
            helperImage.setVisibility(View.VISIBLE);
            upArrow.setVisibility(View.VISIBLE);
        } else {
            helperImage.setVisibility(View.GONE);
            upArrow.setVisibility(View.GONE);
        }

        helperImage.setImageBitmap(bitmap);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        BarcodeView barcodeView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            barcodeView = itemView.findViewById(R.id.barcode);
        }
    }

    private class CodeListViewAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final ArrayList<String> codes;

        CodeListViewAdapter() {
            Checkout checkout = project.getCheckout();

            for (String code : checkout.getCodes()) {
                encodedCodesGenerator.add(code);
            }

            encodedCodesGenerator.add(project.getShoppingCart());
            codes = encodedCodesGenerator.generate(checkout.getId());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return new ViewHolder(layoutInflater.inflate(R.layout.snabble_item_checkout_offline_qrcode, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int margin = Math.round(16 * getResources().getDisplayMetrics().density);
            if (maxSizeMm > 0) {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                float pixelsPerMmX = dm.xdpi / 25.4f;

                int size = (int) (pixelsPerMmX * maxSizeMm);
                Rect rect = new Rect();
                viewPagerContainer.getGlobalVisibleRect(rect);

                int w = rect.width();
                int h = rect.height();

                if (size > w || size > h) {
                    size = ViewGroup.LayoutParams.MATCH_PARENT;
                }

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        size,
                        Gravity.CENTER);

                lp.leftMargin = margin;
                lp.rightMargin = margin;
                holder.barcodeView.setLayoutParams(lp);
            } else {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
                lp.leftMargin = margin;
                lp.rightMargin = margin;
                holder.barcodeView.setLayoutParams(lp);
            }

            holder.barcodeView.setFormat(BarcodeFormat.QR_CODE);
            holder.barcodeView.setText(codes.get(position));
        }

        @Override
        public int getItemCount() {
            return codes.size();
        }
    }
}
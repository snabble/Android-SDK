package io.snabble.sdk.ui.checkout;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;

import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;

public class CheckoutDoneView extends FrameLayout {
    public CheckoutDoneView(Context context) {
        super(context);
        inflateView();
    }

    public CheckoutDoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CheckoutDoneView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_checkout_done, this);

        if (SnabbleUI.getActionBar() != null) {
            SnabbleUI.getActionBar().setTitle(R.string.Snabble_Checkout_done);
        }

        final ScrollView scrollView = findViewById(R.id.scroll_view);
        View goToHome = findViewById(R.id.goto_home);
        View goToCheckoutOverview = findViewById(R.id.goto_checkout_overview);
        View rating1 = findViewById(R.id.rating_1);
        View rating2 = findViewById(R.id.rating_2);
        View rating3 = findViewById(R.id.rating_3);
        final TextInputLayout inputBadRatingLayout = findViewById(R.id.input_bad_rating_layout);
        final View ratingContainer = findViewById(R.id.rating_container);
        final TextView ratingTitle = findViewById(R.id.rating_title);

        goToHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showHome();
                }
            }
        });

        goToCheckoutOverview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showReceipts();
                }
            }
        });

        rating1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                inputBadRatingLayout.setVisibility(View.VISIBLE);
                ratingContainer.setVisibility(View.GONE);
                ratingTitle.setText("Was war nicht gut?");
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });

        rating2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ratingContainer.setVisibility(View.INVISIBLE);
                ratingTitle.setText("Danke!");
            }
        });

        rating3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ratingContainer.setVisibility(View.INVISIBLE);
                ratingTitle.setText("Danke!");
            }
        });

        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                removeOnLayoutChangeListener(this);
                ViewGroup vg = findViewById(R.id.stars_container);
                animateStars(vg);
            }
        });

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);

                getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }, 2000);
    }

    public void sendRating(int rating) {

    }

    private int dp2px(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void animateStars(final ViewGroup vg) {
        View successImage = findViewById(R.id.success_image);
        successImage.setScaleX(1.1f);
        successImage.setScaleY(1.1f);
        successImage.setAlpha(0.0f);
        successImage.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setInterpolator(new OvershootInterpolator(10.0f))
                .setStartDelay(200)
                .setDuration(200)
                .start();

        Drawable star = getResources().getDrawable(R.drawable.ic_star);
        Drawable success = getResources().getDrawable(R.drawable.ic_success);

        int starCount = 50;

        Random random = new Random(2);

        for (int i = 0; i < starCount; i++) {
            int radius = (int) (success.getIntrinsicHeight() / 2.0f + dp2px(150) * random.nextFloat());
            float angle = 360.0f / starCount * i;

            float rx = (float) (Math.cos(angle) * radius);
            float ry = (float) (Math.sin(angle) * radius);

            final ImageView imageView = new ImageView(getContext());
            imageView.setImageDrawable(star);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            layoutParams.gravity = Gravity.CENTER;
            vg.addView(imageView, layoutParams);

            float scale = 0.5f + 0.5f * random.nextFloat();
            imageView.setTranslationX(rx * 0.3f);
            imageView.setTranslationY(ry * 0.3f);
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            imageView.setVisibility(View.INVISIBLE);

            imageView.animate()
                    .translationX(rx)
                    .translationY(ry)
                    .alpha(0.0f)
                    .setStartDelay(300)
                    .setDuration(1200)
                    .setInterpolator(new DecelerateInterpolator(1.0f + 0.8f * random.nextFloat()))
                    .withStartAction(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setVisibility(View.VISIBLE);
                        }
                    })
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            vg.removeView(imageView);
                        }
                    })
                    .start();

            successImage.bringToFront();
        }
    }
}

package io.snabble.sdk.ui.scanner;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;

import java.util.List;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.Coupon;
import io.snabble.sdk.CouponCode;
import io.snabble.sdk.CouponType;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.Project;
import io.snabble.sdk.Shop;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ViolationNotification;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.checkout.ViolationNotificationUtils;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.I18nUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.ui.views.MessageBoxStackView;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import io.snabble.sdk.utils.Utils;

public class SelfScanningView extends FrameLayout {
    private BarcodeScannerView barcodeScanner;
    private ProductDatabase productDatabase;
    private boolean isInitialized;
    private Button goToCart;
    private View noPermission;
    private boolean isRunning;

    private DelayedProgressDialog progressDialog;
    private long detectAfterTimeMs;
    private ShoppingCart shoppingCart;
    private boolean allowShowingHints;
    private boolean isShowingHint;
    private boolean manualCameraControl;
    private int topDownInfoBoxOffset;
    private MessageBoxStackView messages;
    private ProductConfirmationDialog.Factory productConfirmationDialogFactory;
    private Project project;

    public SelfScanningView(Context context) {
        super(context);
        inflateView();
    }

    public SelfScanningView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public SelfScanningView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_self_scanning, this);

        project = Snabble.getInstance().getCheckedInProject().getValue();

        shoppingCart = project.getShoppingCart();

        messages = findViewById(R.id.messages);
        barcodeScanner = findViewById(R.id.barcode_scanner_view);
        noPermission = findViewById(R.id.no_permission);

        goToCart = findViewById(R.id.goto_cart);
        goToCart.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                showShoppingCart();
            }
        });

        updateCartButton();

        barcodeScanner.setIndicatorOffset(0, Utils.dp2px(getContext(), -36));

        for (BarcodeFormat format : project.getSupportedBarcodeFormats()) {
            barcodeScanner.addBarcodeFormat(format);
        }

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_loadingProductInformation));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener((dialogInterface, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                resumeBarcodeScanner();
                progressDialog.dismiss();

                return true;
            }
            return false;
        });

        this.productDatabase = project.getProductDatabase();

        barcodeScanner.setCallback(this::handleBarcodeDetected);

        isInitialized = true;
        startBarcodeScanner(false);

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        );
    }

    private void showShoppingCart() {
        SnabbleUI.executeAction(getContext(), SnabbleUI.Event.SHOW_SHOPPING_CART);
    }

    private void updateCartButton() {
        PriceFormatter priceFormatter = project.getPriceFormatter();

        if (shoppingCart.size() > 0) {
            goToCart.setVisibility(View.VISIBLE);

            if (shoppingCart.getTotalPrice() != 0) {
                goToCart.setText(getResources().getString(R.string.Snabble_Scanner_goToCart, priceFormatter.format(shoppingCart.getTotalPrice())));
            } else {
                goToCart.setText(getResources().getString(R.string.Snabble_Scanner_goToCart_empty));
            }
        } else {
            goToCart.setVisibility(View.INVISIBLE);
        }
    }

    public void lookupAndShowProduct(List<ScannedCode> scannedCodes, BarcodeFormat barcodeFormat) {
        new ProductResolver.Builder(getContext())
                .setCodes(scannedCodes)
                .setBarcodeFormat(barcodeFormat)
                .setOnShowListener(this::pauseBarcodeScanner)
                .setOnDismissListener(() -> {
                    if (!isShowingHint) {
                        delayNextScan();
                        resumeBarcodeScanner();
                    }
                })
                .setOnProductNotFoundListener(() ->
                        handleCoupon(scannedCodes, getResources().getString(I18nUtils.getIdentifier(getResources(), R.string.Snabble_Scanner_unknownBarcode))))
                .setOnNetworkErrorListener(() ->
                        handleCoupon(scannedCodes, getResources().getString(R.string.Snabble_Scanner_networkError)))
                .setOnShelfCodeScannedListener(() ->
                        showWarning(getResources().getString(I18nUtils.getIdentifier(getResources(), R.string.Snabble_Scanner_scannedShelfCode))))
                .setOnSaleStopListener(() -> new AlertDialog.Builder(getContext())
                        .setTitle(I18nUtils.getIdentifier(getResources(), R.string.Snabble_saleStop_errorMsg_title))
                        .setMessage(I18nUtils.getIdentifier(getResources(), R.string.Snabble_saleStop_errorMsg_scan))
                        .setPositiveButton(R.string.Snabble_OK, null)
                        .setOnDismissListener(dialog -> resumeBarcodeScanner())
                        .setCancelable(false)
                        .create()
                        .show())
                .setOnAgeNotReachedListener(() ->
                        showWarning(getResources().getString(I18nUtils.getIdentifier(getResources(),
                                R.string.Snabble_Scanner_scannedAgeRestrictedProduct))))
                .setOnAlreadyScannedListener(() ->
                        showWarning(getResources().getString(I18nUtils.getIdentifier(getResources(),
                                R.string.Snabble_Scanner_duplicateDepositScanned))))
                .setOnNotForSaleListener(product -> {
                    if (product.getScanMessage() != null) {
                        showScanMessage(product, true);
                    } else {
                        showWarning(getResources().getString(I18nUtils.getIdentifier(getResources(),
                                R.string.Snabble_notForSale_errorMsg_scan)));
                    }
                })
                .setDialogConfirmationDialogFactory(productConfirmationDialogFactory)
                .create()
                .resolve();
    }

    private void handleCoupon(List<ScannedCode> scannedCodes, String failureMessage) {
        Pair<Coupon, ScannedCode> coupon = lookupCoupon(scannedCodes);
        if (coupon == null) {
            showWarning(failureMessage);
        } else {
            if (!shoppingCart.containsScannedCode(coupon.second)) {
                shoppingCart.add(shoppingCart.newItem(coupon.first, coupon.second));
            }

            showInfo(getResources().getString(R.string.Snabble_Scanner_couponAdded, coupon.first.getName()));
        }
    }

    private Pair<Coupon, ScannedCode> lookupCoupon(List<ScannedCode> scannedCodes) {
        for (Coupon coupon : project.getCoupons().filter(CouponType.PRINTED)) {
            for (CouponCode code : coupon.getCodes()) {
                for (ScannedCode scannedCode : scannedCodes) {
                    if (scannedCode.getCode().equals(code.getCode())
                            && scannedCode.getTemplateName().equals(code.getTemplate())) {
                        return new Pair<>(coupon, scannedCode);
                    }
                }
            }
        }

        return null;
    }

    public void lookupAndShowProduct(List<ScannedCode> scannedCodes) {
        lookupAndShowProduct(scannedCodes, null);
    }

    private void delayNextScan() {
        detectAfterTimeMs = SystemClock.elapsedRealtime() + 2000;
    }

    @SuppressLint("MissingPermission")
    private void handleBarcodeDetected(final Barcode barcode) {
        if (SystemClock.elapsedRealtime() > detectAfterTimeMs) {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.VIBRATE)
                    == PackageManager.PERMISSION_GRANTED) {
                vibrator.vibrate(500L);
            }

            lookupAndShowProduct(ScannedCode.parse(project, barcode.getText()), barcode.getFormat());
        }
    }

    private void showInfo(final String text) {
        Dispatch.mainThread(() -> messages.show(
                text,
                UIUtils.getDurationByLength(text),
                ResourcesCompat.getColor(getResources(), R.color.snabble_infoColor, null),
                ResourcesCompat.getColor(getResources(), R.color.snabble_infoTextColor, null)
        ));
    }

    private void showWarning(final String text) {
        Dispatch.mainThread(() -> messages.show(
                text,
                UIUtils.getDurationByLength(text),
                ResourcesCompat.getColor(getResources(), R.color.snabble_infoColorWarning, null),
                ResourcesCompat.getColor(getResources(), R.color.snabble_infoTextColorWarning, null)
        ));
    }

    public void setDefaultButtonVisibility(boolean visible) {
        findViewById(R.id.bottom_bar).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public int getTopDownInfoBoxOffset() {
        return topDownInfoBoxOffset;
    }

    public void setTopDownInfoBoxOffset(int topDownInfoBoxOffset) {
        this.topDownInfoBoxOffset = topDownInfoBoxOffset;
    }

    public void setTorchEnabled(boolean enabled) {
        barcodeScanner.setTorchEnabled(enabled);
        Telemetry.event(Telemetry.Event.ToggleTorch);
    }

    public boolean isTorchEnabled() {
        return barcodeScanner.isTorchEnabled();
    }

    public void searchWithBarcode() {
        if (productDatabase.isAvailableOffline() && productDatabase.isUpToDate()) {
            SnabbleUI.executeAction(getContext(), SnabbleUI.Event.SHOW_BARCODE_SEARCH);
        } else {
            pauseBarcodeScanner();

            final EditText input = new EditText(getContext());
            MarginLayoutParams lp = new MarginLayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);

            new AlertDialog.Builder(getContext())
                    .setView(input)
                    .setTitle(R.string.Snabble_Scanner_enterBarcode)
                    .setPositiveButton(R.string.Snabble_Done, (dialog, which) -> lookupAndShowProduct(ScannedCode.parse(project, input.getText().toString())))
                    .setNegativeButton(R.string.Snabble_Cancel, null)
                    .setOnDismissListener(dialog -> resumeBarcodeScanner())
                    .create()
                    .show();

            input.requestFocus();

            Dispatch.mainThread(() -> {
                InputMethodManager inputMethodManager = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    public void setAllowShowingHints(boolean allowShowingHints) {
        this.allowShowingHints = allowShowingHints;
    }

    private void showHints() {
        if (allowShowingHints) {
            Shop currentShop = Snabble.getInstance().getCheckedInShop();

            if (currentShop != null) {
                pauseBarcodeScanner();

                Context context = getContext();

                final AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.Snabble_Hints_title, currentShop.getName()))
                        .setMessage(context.getString(R.string.Snabble_Hints_closedBags))
                        .setPositiveButton(R.string.Snabble_OK, null)
                        .setCancelable(true)
                        .setOnDismissListener(dialog -> {
                            resumeBarcodeScanner();
                            isShowingHint = false;
                        })
                        .create();

                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                isShowingHint = true;
            }
        }
    }

    private void showScanMessage(Product product, boolean allowFallback) {
        Resources res = getResources();

        String identifier = product.getScanMessage();
        if (identifier != null) {
            // replace occurences of "-" in scan message, as android resource identifiers are not
            // supporting "-" in identifiers
            String idWithoutProjectId = identifier.replace("-", ".");
            String idWithProjectId = project.getId().replace("-", ".") + "." + idWithoutProjectId;

            int resId = res.getIdentifier(idWithProjectId, "string", getContext().getPackageName());

            if (resId == 0) {
                resId = res.getIdentifier(idWithoutProjectId, "string", getContext().getPackageName());
            }

            if (resId != 0) {
                String str = res.getString(resId);
                showInfo(str);
            } else {
                if (allowFallback) {
                    showWarning(getResources().getString(I18nUtils.getIdentifier(getResources(),
                            R.string.Snabble_Scanner_unknownBarcode)));
                }
            }
        }
    }

    public void resume() {
        resumeBarcodeScanner();
    }

    public void pause() {
        pauseBarcodeScanner();
    }

    private void pauseBarcodeScanner() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeScanner.pause();
        }
    }

    private void resumeBarcodeScanner() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeScanner.resume();
        }
    }

    /**
     * Setting this to true, makes you the controller of the camera,
     * with the use of startScanning and stopScanning.
     *
     * Default is false which means the view controls the camera by itself
     * when it attached and detaches itself of the window
     */
    public void setManualCameraControl(boolean b) {
        manualCameraControl = b;
    }

    public void startScanning() {
        if (manualCameraControl) {
            startBarcodeScanner(true);
        }
    }

    public void stopScanning() {
        if (manualCameraControl) {
            stopBarcodeScanner(true);
        }
    }

    private void startBarcodeScanner(boolean force) {
        if (manualCameraControl && !force) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (isInitialized) {
                noPermission.setVisibility(View.GONE);
                barcodeScanner.setVisibility(View.VISIBLE);
                barcodeScanner.start();
            }
        } else if (isRunning) {
            noPermission.setVisibility(View.VISIBLE);
            barcodeScanner.setVisibility(View.GONE);
        }
    }

    private void stopBarcodeScanner(boolean force) {
        if (manualCameraControl && !force) {
            return;
        }

        if (isInitialized) {
            barcodeScanner.stop();
        }
    }

    public void addBarcodeFormat(BarcodeFormat barcodeFormat) {
        barcodeScanner.addBarcodeFormat(barcodeFormat);
    }

    public void removeBarcodeFormat(BarcodeFormat barcodeFormat) {
        barcodeScanner.removeBarcodeFormat(barcodeFormat);
    }

    /**
     * Sets the offset of the scan indicator, in pixels.
     */
    public void setIndicatorOffset(int offsetX, int offsetY) {
        barcodeScanner.setIndicatorOffset(offsetX, offsetY);
    }

    private void registerListeners() {
        isRunning = true;

        startBarcodeScanner(false);
        shoppingCart.addListener(shoppingCartListener);
        updateCartButton();
    }

    private void unregisterListeners() {
        isRunning = false;

        stopBarcodeScanner(false);

        progressDialog.dismiss();
        shoppingCart.removeListener(shoppingCartListener);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        registerListeners();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        unregisterListeners();
    }

    public BarcodeDetector getBarcodeDetector() {
        return barcodeScanner.getBarcodeDetector();
    }

    public void setProductConfirmationDialogFactory(ProductConfirmationDialog.Factory productConfirmationDialogFactory) {
        this.productConfirmationDialogFactory = productConfirmationDialogFactory;
    }

    private final ShoppingCart.ShoppingCartListener shoppingCartListener = new ShoppingCart.SimpleShoppingCartListener() {
        @Override
        public void onItemAdded(ShoppingCart list, ShoppingCart.Item item) {
            super.onItemAdded(list, item);

            if (list.getAddCount() == 1) {
                showHints();
            }
        }

        @Override
        public void onQuantityChanged(ShoppingCart list, ShoppingCart.Item item) {
            showScanMessage(item.getProduct(), false);
        }

        @Override
        public void onChanged(ShoppingCart list) {
            updateCartButton();
        }

        @Override
        public void onCheckoutLimitReached(ShoppingCart list) {
            showInfo(getResources().getString(R.string.Snabble_limitsAlert_checkoutNotAvailable,
                    project.getPriceFormatter().format(project.getMaxCheckoutLimit())));
        }

        @Override
        public void onOnlinePaymentLimitReached(ShoppingCart list) {
            showInfo(getResources().getString(R.string.Snabble_limitsAlert_notAllMethodsAvailable,
                    project.getPriceFormatter().format(project.getMaxOnlinePaymentLimit())));

        }

        @Override
        public void onViolationDetected(@NonNull List<ViolationNotification> violations) {
            ViolationNotificationUtils.showNotificationOnce(violations, getContext(), shoppingCart);
        }
    };

    private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        registerListeners();
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        unregisterListeners();
                    }
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        startBarcodeScanner(false);
                    }
                }
            };
}
package io.snabble.sdk.ui.scanner;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.List;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.Project;
import io.snabble.sdk.Shop;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.I18nUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import io.snabble.sdk.utils.Utils;

public class SelfScanningView extends FrameLayout {
    private BarcodeScannerView barcodeScanner;
    private ProductDatabase productDatabase;
    private boolean isInitialized;
    private ImageView enterBarcode;
    private ImageView light;
    private Button goToCart;
    private View noPermission;
    private boolean isRunning;

    private DelayedProgressDialog progressDialog;
    private long detectAfterTimeMs;
    private ShoppingCart shoppingCart;
    private boolean allowShowingHints;
    private boolean isShowingHint;
    private boolean manualCameraControl;

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

        Project project = SnabbleUI.getProject();

        shoppingCart = project.getShoppingCart();

        barcodeScanner = findViewById(R.id.barcode_scanner_view);
        noPermission = findViewById(R.id.no_permission);

        enterBarcode = findViewById(R.id.enter_barcode);
        light = findViewById(R.id.light);
        light.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeScanner.setTorchEnabled(!barcodeScanner.isTorchEnabled());
                updateTorchIcon();
                Telemetry.event(Telemetry.Event.ToggleTorch);
            }
        });

        goToCart = findViewById(R.id.goto_cart);
        goToCart.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                showShoppingCart();
            }
        });

        updateBarcodeSearchIcon();
        updateTorchIcon();
        updateCartButton();

        enterBarcode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickEnterBarcode();
            }
        });

        barcodeScanner.setIndicatorOffset(0, Utils.dp2px(getContext(), -36));
        barcodeScanner.setRestrictionOvershoot(1.15f);

        for (BarcodeFormat format : project.getSupportedBarcodeFormats()) {
            barcodeScanner.addBarcodeFormat(format);
        }

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_loadingProductInformation));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    resumeBarcodeScanner();
                    progressDialog.dismiss();

                    return true;
                }
                return false;
            }
        });

        this.productDatabase = project.getProductDatabase();

        barcodeScanner.setCallback(new BarcodeScannerView.Callback() {
            @Override
            public void onBarcodeDetected(final Barcode barcode) {
                handleBarcodeDetected(barcode);
            }
        });

        isInitialized = true;
        startBarcodeScanner(false);
    }

    private void showShoppingCart() {
        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            callback.execute(SnabbleUI.Action.SHOW_SHOPPING_CART, null);
        }
    }

    private void updateBarcodeSearchIcon() {
        enterBarcode.setImageResource(R.drawable.snabble_ic_search);
        ViewCompat.setBackgroundTintList(enterBarcode, ColorStateList.valueOf(Color.WHITE));
        ImageViewCompat.setImageTintList(enterBarcode, ColorStateList.valueOf(Color.WHITE));
    }

    private int dp2px(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void updateTorchIcon() {
        if (barcodeScanner.isTorchEnabled()) {
            light.setImageResource(R.drawable.snabble_ic_torch_active);
            light.setBackgroundResource(R.drawable.snabble_ic_button_filled_48dp);
            UIUtils.getColorByAttribute(getContext(), R.attr.colorPrimary);
            int color = UIUtils.getColorByAttribute(getContext(), R.attr.colorPrimary);
            ImageViewCompat.setImageTintList(light, ColorStateList.valueOf(color));
        } else {
            light.setImageResource(R.drawable.snabble_ic_torch);
            light.setBackgroundResource(R.drawable.snabble_ic_button_outlined_48dp);
            ViewCompat.setBackgroundTintList(light, ColorStateList.valueOf(Color.WHITE));
            ImageViewCompat.setImageTintList(light, ColorStateList.valueOf(Color.WHITE));
        }

        int dp = dp2px(12);
        light.setPadding(dp, dp, dp, dp);
    }

    private void updateCartButton() {
        PriceFormatter priceFormatter = SnabbleUI.getProject().getPriceFormatter();

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
        final int modCount = shoppingCart.getModCount();

        new ProductResolver.Builder(getContext())
                .setCodes(scannedCodes)
                .setBarcodeFormat(barcodeFormat)
                .setOnShowListener(new ProductResolver.OnShowListener() {
                    @Override
                    public void onShow() {
                        pauseBarcodeScanner();
                    }
                })
                .setOnDismissListener(new ProductResolver.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        if (!isShowingHint) {
                            resumeBarcodeScanner();
                            delayNextScan();
                        }
                    }
                })
                .setOnProductNotFoundListener(new ProductResolver.OnProductNotFoundListener() {
                    @Override
                    public void onProductNotFound() {
                        showWarning(getResources().getString(I18nUtils.getIdentifier(getResources(), R.string.Snabble_Scanner_unknownBarcode)));
                    }
                })
                .setOnNetworkErrorListener(new ProductResolver.OnNetworkErrorListener() {
                    @Override
                    public void onNetworkError() {
                        showWarning(getResources().getString(R.string.Snabble_Scanner_networkError));
                    }
                })
                .setOnShelfCodeScannedListener(new ProductResolver.OnShelfCodeScannedListener() {
                    @Override
                    public void onShelfCodeScanned() {
                        showWarning(getResources().getString(I18nUtils.getIdentifier(getResources(), R.string.Snabble_Scanner_scannedShelfCode)));
                    }
                })
                .setOnSaleStopListener(new ProductResolver.OnSaleStopListener() {
                    @Override
                    public void onSaleStop() {
                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.Snabble_saleStop_errorMsg_title)
                                .setMessage(R.string.Snabble_saleStop_errorMsg_scan)
                                .setPositiveButton(R.string.Snabble_OK, null)
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        resumeBarcodeScanner();
                                    }
                                })
                                .setCancelable(false)
                                .create()
                                .show();
                    }
                })
                .create()
                .show();
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

            lookupAndShowProduct(ScannedCode.parse(SnabbleUI.getProject(), barcode.getText()), barcode.getFormat());
        }
    }

    private void showInfo(final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                UIUtils.showTopDownInfoBox(SelfScanningView.this, text,
                        UIUtils.getDurationByLength(text), UIUtils.INFO_NEUTRAL);
            }
        });
    }

    private void showWarning(final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                UIUtils.showTopDownInfoBox(SelfScanningView.this, text,
                        UIUtils.getDurationByLength(text), UIUtils.INFO_WARNING);
            }
        });
    }

    private void onClickEnterBarcode() {
        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            if (productDatabase.isAvailableOffline() && productDatabase.isUpToDate()) {
                callback.execute(SnabbleUI.Action.SHOW_BARCODE_SEARCH, null);
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
                        .setPositiveButton(R.string.Snabble_Done, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                lookupAndShowProduct(ScannedCode.parse(SnabbleUI.getProject(), input.getText().toString()));
                            }
                        })
                        .setNegativeButton(R.string.Snabble_Cancel, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                resumeBarcodeScanner();
                            }
                        })
                        .create()
                        .show();

                input.requestFocus();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager inputMethodManager = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        }
    }

    public void setAllowShowingHints(boolean allowShowingHints) {
        this.allowShowingHints = allowShowingHints;
    }

    private void showHints() {
        if (allowShowingHints) {
            Project project = SnabbleUI.getProject();
            Shop currentShop = project.getCheckedInShop();

            if (currentShop != null) {
                pauseBarcodeScanner();

                Context context = getContext();

                final AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.Snabble_Hints_title, currentShop.getName()))
                        .setMessage(context.getString(R.string.Snabble_Hints_closedBags))
                        .setPositiveButton(R.string.Snabble_OK, null)
                        .setCancelable(true)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                resumeBarcodeScanner();
                                isShowingHint = false;
                            }
                        })
                        .create();

                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();
                isShowingHint = true;
            }
        }
    }

    private void showScanMessage(Product product) {
        Project project = SnabbleUI.getProject();
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

    private ShoppingCart.ShoppingCartListener shoppingCartListener = new ShoppingCart.SimpleShoppingCartListener() {
        @Override
        public void onItemAdded(ShoppingCart list, ShoppingCart.Item item) {
            super.onItemAdded(list, item);

            if (list.getAddCount() == 1) {
                showHints();
            }
        }

        @Override
        public void onQuantityChanged(ShoppingCart list, ShoppingCart.Item item) {
            showScanMessage(item.getProduct());
        }

        @Override
        public void onChanged(ShoppingCart list) {
            updateCartButton();
        }

        @Override
        public void onCheckoutLimitReached(ShoppingCart list) {
            Project project = SnabbleUI.getProject();
            showInfo(getResources().getString(R.string.Snabble_limitsAlert_checkoutNotAvailable,
                    project.getPriceFormatter().format(project.getMaxCheckoutLimit())));
        }

        @Override
        public void onOnlinePaymentLimitReached(ShoppingCart list) {
            Project project = SnabbleUI.getProject();
            showInfo(getResources().getString(R.string.Snabble_limitsAlert_notAllMethodsAvailable,
                    project.getPriceFormatter().format(project.getMaxOnlinePaymentLimit())));

        }
    };

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
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

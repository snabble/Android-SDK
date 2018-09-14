package io.snabble.sdk.ui.scanner;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.OnProductAvailableListener;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.Project;
import io.snabble.sdk.Shop;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import io.snabble.sdk.utils.Utils;

public class SelfScanningView extends CoordinatorLayout implements Checkout.OnCheckoutStateChangedListener {
    private BarcodeScannerView barcodeScanner;
    private ProductDatabase productDatabase;
    private boolean isInitialized;
    private ProductConfirmationDialog productDialog;
    private boolean allowScan = true;
    private View enterBarcode;
    private View noPermission;
    private boolean isRunning;

    private DelayedProgressDialog progressDialog;
    private long detectAfterTimeMs;
    private boolean ignoreNextDialog;
    private ShoppingCart shoppingCart;
    private boolean allowShowingHints;
    private TextView info;
    private Handler infoHandler = new Handler(Looper.getMainLooper());
    private boolean isShowingHint;

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
        inflate(getContext(), R.layout.view_self_scanning, this);

        Project project = SnabbleUI.getProject();

        shoppingCart = project.getShoppingCart();

        barcodeScanner = findViewById(R.id.barcode_scanner_view);
        noPermission = findViewById(R.id.no_permission);
        info = findViewById(R.id.info);
        info.setVisibility(View.INVISIBLE);
        info.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                info.setTranslationY(-info.getHeight());
                info.setVisibility(View.VISIBLE);
            }
        });

        enterBarcode = findViewById(R.id.enter_barcode);
        TextView light = findViewById(R.id.light);

        light.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeScanner.setTorchEnabled(!barcodeScanner.isTorchEnabled());
                Telemetry.event(Telemetry.Event.ToggleTorch);
            }
        });

        enterBarcode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickEnterBarcode();
            }
        });

        barcodeScanner.setIndicatorOffset(0, Utils.dp2px(getContext(), -36));

        barcodeScanner.addBarcodeFormat(BarcodeFormat.EAN_8);
        barcodeScanner.addBarcodeFormat(BarcodeFormat.EAN_13);
        barcodeScanner.addBarcodeFormat(BarcodeFormat.CODE_128);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_loadingProductInformation));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    resumeBarcodeScanner();
                    progressDialog.dismiss();

                    ignoreNextDialog = true;

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

        productDialog = new ProductConfirmationDialog(getContext(), project);
        productDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(!isShowingHint) {
                    resumeBarcodeScanner();
                }

                allowScan = true;
            }
        });

        productDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                allowScan = false;
                pauseBarcodeScanner();
            }
        });

        isInitialized = true;
        startBarcodeScanner();
    }

    public void lookupAndShowProduct(final ScannableCode scannedCode) {
        productDialog.dismiss();
        ignoreNextDialog = false;
        pauseBarcodeScanner();

        if(scannedCode.hasEmbeddedData() && !scannedCode.isEmbeddedDataOk()){
            resumeBarcodeScanner();
            delayNextScan();

            Telemetry.event(Telemetry.Event.ScannedUnknownCode, scannedCode.getCode());
            showInfo(R.string.Snabble_Scanner_unknownBarcode);
            return;
        }

        progressDialog.showAfterDelay(300);

        if(scannedCode.hasEmbeddedData()){
            productDatabase.findByWeighItemIdOnline(scannedCode.getLookupCode(), new OnProductAvailableListener() {
                @Override
                public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                    handleProductAvailable(product, wasOnlineProduct, scannedCode);
                }

                @Override
                public void onProductNotFound() {
                    handleProductNotFound(scannedCode);
                }

                @Override
                public void onError() {
                    handleProductError();
                }
            });
        } else {
            productDatabase.findByCodeOnline(scannedCode.getCode(), new OnProductAvailableListener() {
                @Override
                public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                    handleProductAvailable(product, wasOnlineProduct, scannedCode);
                }

                @Override
                public void onProductNotFound() {
                    handleProductNotFound(scannedCode);
                }

                @Override
                public void onError() {
                    handleProductError();
                }
            });
        }
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

            lookupAndShowProduct(ScannableCode.parse(SnabbleUI.getProject(), barcode.getText()));
        }
    }

    private void handleProductAvailable(Product product, boolean wasOnlineProduct, ScannableCode scannedCode) {
        if (ignoreNextDialog) {
            return;
        }

        progressDialog.dismiss();

        if(product.getBundleProducts().length > 0){
            showBundleDialog(product);
        } else {
            if (product.getType() == Product.Type.PreWeighed && !scannedCode.hasEmbeddedData()) {
                showInfo(R.string.Snabble_Scanner_scannedShelfCode);

                progressDialog.dismiss();
                resumeBarcodeScanner();
                delayNextScan();
            } else {
                showProduct(product, scannedCode);

                if (wasOnlineProduct) {
                    Telemetry.event(Telemetry.Event.ScannedOnlineProduct, product);
                } else {
                    Telemetry.event(Telemetry.Event.ScannedProduct, product);
                }
            }
        }
    }

    private void handleProductNotFound(ScannableCode scannedCode) {
        progressDialog.dismiss();
        resumeBarcodeScanner();
        delayNextScan();

        Telemetry.event(Telemetry.Event.ScannedUnknownCode, scannedCode.getCode());

        showInfo(R.string.Snabble_Scanner_unknownBarcode);
    }

    private void handleProductError() {
        progressDialog.dismiss();
        resumeBarcodeScanner();
        delayNextScan();

        showInfo(R.string.Snabble_Scanner_networkError);
    }

    private void showInfo(@StringRes int resId) {
        info.setVisibility(View.VISIBLE);
        info.setText(resId);
        info.animate().translationY(0).start();

        infoHandler.removeCallbacksAndMessages(null);
        infoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                info.animate().translationY(-info.getHeight()).start();
            }
        }, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
    }

    private void onClickEnterBarcode() {
        SnabbleUICallback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            if(productDatabase.isAvailableOffline() && productDatabase.isUpToDate()){
                callback.showBarcodeSearch();
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
                                lookupAndShowProduct(ScannableCode.parse(SnabbleUI.getProject(),
                                        input.getText().toString()));
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

            }
        }
    }

    public void setAllowShowingHints(boolean allowShowingHints) {
        this.allowShowingHints = allowShowingHints;
    }

    private void showHints() {
        if(allowShowingHints) {
            Project project = SnabbleUI.getProject();
            Shop currentShop = project.getCheckout().getShop();

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

    private void showBundleDialog(Product product) {
        pauseBarcodeScanner();
        allowScan = false;

        SelectBundleDialog.show(getContext(), product, new SelectBundleDialog.Callback() {
            @Override
            public void onProductSelected(Product product) {
                Telemetry.event(Telemetry.Event.SelectedBundleProduct, product);

                String[] codes = product.getScannableCodes();
                if(codes.length > 0) {
                    showProduct(product, ScannableCode.parse(SnabbleUI.getProject(), codes[0]));
                }
            }

            @Override
            public void onDismissed() {
                resumeBarcodeScanner();
            }
        });
    }

    private void showProduct(Product product, ScannableCode scannedCode) {
        pauseBarcodeScanner();
        allowScan = false;
        showProductDialog(product, scannedCode);
    }

    public void hideProduct() {
        productDialog.dismiss();
    }

    protected void showProductDialog(Product product, ScannableCode scannedCode) {
        productDialog.show(product, scannedCode);
    }

    private void startBarcodeScanner() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (isInitialized && allowScan) {
                noPermission.setVisibility(View.GONE);
                barcodeScanner.setVisibility(View.VISIBLE);
                barcodeScanner.start();
            }
        } else if (isRunning) {
            noPermission.setVisibility(View.VISIBLE);
            barcodeScanner.setVisibility(View.GONE);
        }
    }

    private void stopBarcodeScanner() {
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

    public void registerListeners() {
        isRunning = true;

        startBarcodeScanner();
        shoppingCart.addListener(shoppingCartListener);
    }

    public void unregisterListeners() {
        isRunning = false;

        stopBarcodeScanner();

        progressDialog.dismiss();
        productDialog.dismiss();
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

    private ShoppingCart.ShoppingCartListener shoppingCartListener = new ShoppingCart.ShoppingCartListener() {
        @Override
        public void onItemAdded(ShoppingCart list, Product product) {
            if(list.getAddCount() == 1) {
                showHints();
            }
        }

        @Override
        public void onQuantityChanged(ShoppingCart list, Product product) {

        }

        @Override
        public void onCleared(ShoppingCart list) {

        }

        @Override
        public void onItemMoved(ShoppingCart list, int fromIndex, int toIndex) {

        }

        @Override
        public void onItemRemoved(ShoppingCart list, Product product) {

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
                        startBarcodeScanner();
                    }
                }
            };


    @Override
    public void onStateChanged(Checkout.State state) {
        if (!isShown()) {
            return;
        }

        switch (state) {
            case HANDSHAKING:
                progressDialog.showAfterDelay(500);
                break;
            case REQUEST_PAYMENT_METHOD:
            case WAIT_FOR_APPROVAL:
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showCheckout();
                }

                progressDialog.dismiss();
                break;
            case CONNECTION_ERROR:
                progressDialog.dismiss();

                UIUtils.snackbar(SelfScanningView.this,
                        R.string.Snabble_Payment_errorStarting,
                        UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .show();
                break;
            default:
                progressDialog.dismiss();
        }
    }
}

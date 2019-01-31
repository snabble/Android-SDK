package io.snabble.sdk.ui.scanner;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.KeyEvent;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.OnProductAvailableListener;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;

public class ProductResolver {
    private List<ScannableCode> scannableCodes;
    private ProductConfirmationDialog productConfirmationDialog;
    private DelayedProgressDialog progressDialog;
    private Context context;
    private OnShowListener onShowListener;
    private OnDismissListener onDismissListener;
    private OnShelfCodeScannedListener onShelfCodeScannedListener;
    private OnProductNotFoundListener onProductNotFoundListener;
    private OnNetworkErrorListener onNetworkErrorListener;
    private BarcodeFormat barcodeFormat;

    public ProductResolver(Context context) {
        this.context = context;

        productConfirmationDialog = new ProductConfirmationDialog(context, SnabbleUI.getProject());
        productConfirmationDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(onDismissListener != null) {
                    onDismissListener.onDismiss();
                }
            }
        });

        progressDialog = new DelayedProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(context.getString(R.string.Snabble_loadingProductInformation));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    progressDialog.dismiss();
                    return true;
                }
                return false;
            }
        });
    }

    public List<ScannableCode> getScannableCodes() {
        return scannableCodes;
    }

    public BarcodeFormat getBarcodeFormat() {
        return barcodeFormat;
    }

    private class Result {
        Product product;
        boolean wasOnlineProduct;
        ScannableCode code;
        boolean error;
        int matchCount;
    }

    private void lookupAndShowProduct(final List<ScannableCode> scannedCodes) {
        productConfirmationDialog.dismiss();
        progressDialog.showAfterDelay(300);

        if (onShowListener != null) {
            onShowListener.onShow();
        }

        final Project project = SnabbleUI.getProject();
        final ProductDatabase productDatabase = project.getProductDatabase();

        HandlerThread handlerThread = new HandlerThread("ProductResolver");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                final CountDownLatch countDownLatch = new CountDownLatch(scannedCodes.size());
                final Result result = new Result();

                for (int i=0; i<scannedCodes.size(); i++) {
                    final ScannableCode scannableCode = scannedCodes.get(i);
                    productDatabase.findByCodeOnline(scannableCode, new OnProductAvailableListener() {
                        @Override
                        public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                            result.product = product;
                            result.wasOnlineProduct = wasOnlineProduct;
                            result.code = scannableCode;
                            result.matchCount++;
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onProductNotFound() {
                            if (result.code == null) {
                                result.code = scannableCode;
                            }

                            countDownLatch.countDown();
                        }

                        @Override
                        public void onError() {
                            result.error = true;
                            countDownLatch.countDown();
                        }
                    });
                }

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                Handler mainThread = new Handler(Looper.getMainLooper());
                mainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        if (result.matchCount > 1) {
                            project.logErrorEvent("Multiple code matches for product " + result.product.getSku());
                        }

                        if (result.product != null) {
                            handleProductAvailable(result.product, result.wasOnlineProduct, result.code);
                        } else if (result.error) {
                            handleProductError();
                        } else {
                            handleProductNotFound(result.code);
                        }
                    }
                });
            }
        });
    }

    private void handleProductAvailable(Product product, boolean wasOnlineProduct, ScannableCode scannedCode) {
        progressDialog.dismiss();

        if(product.getBundleProducts().length > 0){
            showBundleDialog(product);
        } else {
            if (product.getType() == Product.Type.PreWeighed && (!scannedCode.hasEmbeddedData() || scannedCode.getEmbeddedData() == 0)) {
                if (onShelfCodeScannedListener != null) {
                    onShelfCodeScannedListener.onShelfCodeScanned();
                }

                progressDialog.dismiss();

                if (onDismissListener != null) {
                    onDismissListener.onDismiss();
                }
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

    private void showBundleDialog(Product product) {
        SelectBundleDialog.show(context, product, new SelectBundleDialog.Callback() {
            @Override
            public void onProductSelected(Product product) {
                Telemetry.event(Telemetry.Event.SelectedBundleProduct, product);

                Product.Code[] codes = product.getScannableCodes();
                if(codes.length > 0) {
                    List<ScannableCode> scannableCodes = ScannableCode.parse(SnabbleUI.getProject(), codes[0].lookupCode);
                    if (scannableCodes != null && scannableCodes.size() > 0) {
                        showProduct(product, scannableCodes.get(0));
                    }
                }
            }

            @Override
            public void onDismissed() {
                if (onDismissListener != null) {
                    onDismissListener.onDismiss();
                }
            }
        });
    }

    private void handleProductNotFound(ScannableCode scannedCode) {
        progressDialog.dismiss();
        Telemetry.event(Telemetry.Event.ScannedUnknownCode, scannedCode.getCode());

        if(onDismissListener != null) {
            onDismissListener.onDismiss();
        }

        if (onProductNotFoundListener != null) {
            onProductNotFoundListener.onProductNotFound();
        }
    }

    private void handleProductError() {
        progressDialog.dismiss();

        if(onDismissListener != null) {
            onDismissListener.onDismiss();
        }

        if (onNetworkErrorListener != null) {
            onNetworkErrorListener.onNetworkError();
        }
    }

    private void showProduct(Product product, ScannableCode scannedCode) {
        productConfirmationDialog.show(product, scannedCode);
    }

    public void show() {
        lookupAndShowProduct(scannableCodes);
    }

    public void dismiss() {
        productConfirmationDialog.dismiss();
    }

    public void setOnShowListener(OnShowListener listener) {
        onShowListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        onDismissListener = listener;
    }

    public interface OnShowListener {
        void onShow();
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    public interface OnShelfCodeScannedListener {
        void onShelfCodeScanned();
    }

    public interface OnProductNotFoundListener {
        void onProductNotFound();
    }

    public interface OnNetworkErrorListener {
        void onNetworkError();
    }

    public static class Builder {
        private ProductResolver productResolver;

        public Builder(Context context) {
            productResolver = new ProductResolver(context);
        }

        public Builder setCodes(List<ScannableCode> codes) {
            productResolver.scannableCodes = codes;
            return this;
        }

        public Builder setBarcodeFormat(BarcodeFormat barcodeFormat) {
            productResolver.barcodeFormat = barcodeFormat;
            return this;
        }

        public Builder setOnShowListener(OnShowListener listener) {
            productResolver.onShowListener = listener;
            return this;
        }

        public Builder setOnDismissListener(OnDismissListener listener) {
            productResolver.onDismissListener = listener;
            return this;
        }

        public Builder setOnProductNotFoundListener(OnProductNotFoundListener listener) {
            productResolver.onProductNotFoundListener = listener;
            return this;
        }

        public Builder setOnNetworkErrorListener(OnNetworkErrorListener listener) {
            productResolver.onNetworkErrorListener  = listener;
            return this;
        }

        public Builder setOnShelfCodeScannedListener(OnShelfCodeScannedListener listener) {
            productResolver.onShelfCodeScannedListener  = listener;
            return this;
        }

        public ProductResolver create() {
            return productResolver;
        }
    }
}

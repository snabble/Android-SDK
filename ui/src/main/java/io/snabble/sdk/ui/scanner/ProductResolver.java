package io.snabble.sdk.ui.scanner;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.OnProductAvailableListener;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.Project;
import io.snabble.sdk.Unit;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.utils.Dispatch;

public class ProductResolver {
    private List<ScannedCode> scannedCodes;
    private ProductConfirmationDialog productConfirmationDialog;
    private DelayedProgressDialog progressDialog;
    private Context context;
    private OnShowListener onShowListener;
    private OnDismissListener onDismissListener;
    private OnSaleStopListener onSaleStopListener;
    private OnShelfCodeScannedListener onShelfCodeScannedListener;
    private OnProductNotFoundListener onProductNotFoundListener;
    private OnNetworkErrorListener onNetworkErrorListener;
    private BarcodeFormat barcodeFormat;
    private DialogInterface.OnKeyListener onKeyListener;

    private ProductResolver(Context context) {
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

        productConfirmationDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (onKeyListener != null) {
                    return onKeyListener.onKey(dialog, keyCode, event);
                }

                return false;
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

    public List<ScannedCode> getScannedCodes() {
        return scannedCodes;
    }

    public BarcodeFormat getBarcodeFormat() {
        return barcodeFormat;
    }

    private class Result {
        Product product;
        boolean wasOnlineProduct;
        ScannedCode code;
        boolean error;
        int matchCount;
    }

    private void lookupAndShowProduct(final List<ScannedCode> scannedCodes) {
        productConfirmationDialog.dismiss(false);
        progressDialog.showAfterDelay(300);

        if (onShowListener != null) {
            onShowListener.onShow();
        }

        final Project project = SnabbleUI.getProject();
        final ProductDatabase productDatabase = project.getProductDatabase();

        // check if its available local first
        if (productDatabase.isUpToDate()) {
            for (ScannedCode scannedCode : scannedCodes) {
                Product product = productDatabase.findByCode(scannedCode);
                if (product != null) {
                    handleProductAvailable(product, false, scannedCode);
                    return;
                }
            }
        }

        // create multiple online requests asynchronously and wait for all to succeed or fail
        Dispatch.background(() -> {
            final CountDownLatch countDownLatch = new CountDownLatch(scannedCodes.size());
            final Result result = new Result();

            for (int i=0; i<scannedCodes.size(); i++) {
                final ScannedCode scannedCode = scannedCodes.get(i);
                productDatabase.findByCodeOnline(scannedCode, new OnProductAvailableListener() {
                    @Override
                    public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                        result.product = product;
                        result.wasOnlineProduct = wasOnlineProduct;
                        result.code = scannedCode;
                        result.matchCount++;
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onProductNotFound() {
                        if (result.code == null) {
                            result.code = scannedCode;
                        }

                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError() {
                        result.error = true;
                        countDownLatch.countDown();
                    }
                }, true);
            }

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Dispatch.mainThread(() -> {
                if (result.matchCount > 1) {
                    project.logErrorEvent("Multiple code matches for product " + result.product.getSku());
                }

                if (result.product != null) {
                    handleProductAvailable(result.product, result.wasOnlineProduct, result.code);
                } else if (result.error) {
                    handleProductError();
                } else {
                    project.getEvents().productNotFound(scannedCodes);
                    handleProductNotFound(result.code);
                }
            });
        });
    }

    private void handleProductAvailable(Product product, boolean wasOnlineProduct, ScannedCode scannedCode) {
        progressDialog.dismiss();

        if (scannedCode.hasEmbeddedDecimalData()) {
            Unit unit = product.getEncodingUnit(scannedCode.getTemplateName(), scannedCode.getLookupCode());
            if (unit != null) {
                BigDecimal decimal = scannedCode.getEmbeddedDecimalData();
                if (unit == Unit.PIECE || unit == Unit.PRICE) {
                    scannedCode.setEmbeddedData(decimal.intValue());
                    scannedCode.setEmbeddedUnit(unit);
                } else {
                    int scale = decimal.scale();
                    Unit fractionalUnit = unit.getFractionalUnit(scale);
                    if (fractionalUnit == null) {
                        fractionalUnit = unit;

                        if (scale > 0) {
                            decimal = decimal.multiply(new BigDecimal(Math.pow(10, scale)));
                        }
                    }

                    BigDecimal converted = Unit.convert(decimal, unit, fractionalUnit);
                    scannedCode.setEmbeddedData(converted.intValue());
                    scannedCode.setEmbeddedUnit(fractionalUnit);
                }
            }
        }

        if(product.getBundleProducts().length > 0){
            showBundleDialog(product, scannedCode);
        } else {
//            if (product.getSaleStop()) {
//                if (onSaleStopListener != null) {
//                    onSaleStopListener.onSaleStop();
//                }
//
//                progressDialog.dismiss();
//
//                if (onDismissListener != null) {
//                    onDismissListener.onDismiss();
//                }
//            } else
                if (product.getType() == Product.Type.PreWeighed
                    && (!scannedCode.hasEmbeddedData() || scannedCode.getEmbeddedData() == 0)) {
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

    private void showBundleDialog(Product product, final ScannedCode scannedCode) {
        SelectBundleDialog.show(context, product, new SelectBundleDialog.Callback() {
            @Override
            public void onProductSelected(Product product) {
                Telemetry.event(Telemetry.Event.SelectedBundleProduct, product);

                Product.Code[] codes = product.getScannableCodes();
                if (codes.length > 0 && codes[0].lookupCode != null) {
                    List<ScannedCode> newCodes = ScannedCode.parse(SnabbleUI.getProject(), codes[0].lookupCode);
                    if (newCodes.size() > 0) {
                        showProduct(product, newCodes.get(0));
                    } else {
                        showProduct(product, scannedCode);
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

    private void handleProductNotFound(ScannedCode scannedCode) {
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

    private void showProduct(Product product, ScannedCode scannedCode) {
        productConfirmationDialog.show(product, scannedCode);
    }

    public void show() {
        lookupAndShowProduct(scannedCodes);
    }

    public void dismiss() {
        productConfirmationDialog.dismiss(false);
    }

    public void setOnShowListener(OnShowListener listener) {
        onShowListener = listener;
    }

    public void setOnDismissListener(OnDismissListener listener) {
        onDismissListener = listener;
    }

    public void addResolvedItemToCart() {
        productConfirmationDialog.addToCart();
    }

    public interface OnShowListener {
        void onShow();
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    public interface OnSaleStopListener {
        void onSaleStop();
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

        public Builder setCodes(List<ScannedCode> codes) {
            productResolver.scannedCodes = codes;
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

        public Builder setOnKeyListener(DialogInterface.OnKeyListener listener) {
            productResolver.onKeyListener = listener;
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
            productResolver.onNetworkErrorListener = listener;
            return this;
        }

        public Builder setOnSaleStopListener(OnSaleStopListener listener) {
            productResolver.onSaleStopListener = listener;
            return this;
        }

        public Builder setOnShelfCodeScannedListener(OnShelfCodeScannedListener listener) {
            productResolver.onShelfCodeScannedListener = listener;
            return this;
        }


        public ProductResolver create() {
            return productResolver;
        }
    }
}

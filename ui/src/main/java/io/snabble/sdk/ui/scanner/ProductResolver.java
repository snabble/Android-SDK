package io.snabble.sdk.ui.scanner;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.widget.Toast;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.OnProductAvailableListener;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.utils.IntRange;

public class ProductResolver {
    private String scannableCode;
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

    public String getScannableCode() {
        return scannableCode;
    }

    public BarcodeFormat getBarcodeFormat() {
        return barcodeFormat;
    }

    private void lookupAndShowProduct(final ScannableCode scannedCode) {
        productConfirmationDialog.dismiss();
        progressDialog.showAfterDelay(300);

        if (onShowListener != null) {
            onShowListener.onShow();
        }

        ProductDatabase productDatabase = SnabbleUI.getProject().getProductDatabase();

        if(scannedCode.hasEmbeddedData() && scannedCode.getMaskedCode().length() > 0) {
            productDatabase.findByWeighItemIdOnline(scannedCode.getMaskedCode(), new OnProductAvailableListener() {
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

            // TODO check if that is still needed, may be covered by new code templates
//            String lookupCode = scannedCode.getLookupCode();
//            if (barcodeFormat != null) {
//                IntRange range = SnabbleUI.getProject().getRangeForBarcodeFormat(barcodeFormat);
//                if (range != null) {
//                    lookupCode = lookupCode.substring(range.min, range.max);
//                }
//            }

            productDatabase.findByCodeOnline(scannedCode, new OnProductAvailableListener() {
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
                    showProduct(product, ScannableCode.parse(SnabbleUI.getProject(), codes[0].lookupCode));
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
        lookupAndShowProduct(ScannableCode.parse(SnabbleUI.getProject(), scannableCode));
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

        public Builder setCode(String code) {
            productResolver.scannableCode = code;
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

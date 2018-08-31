package io.snabble.sdk.ui.scanner;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import io.snabble.sdk.OnProductAvailableListener;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;

public class ProductResolver {
    private View snackbarHost;
    private String scannableCode;
    private ProductConfirmationDialog productConfirmationDialog;
    private DelayedProgressDialog progressDialog;
    private Context context;
    private OnShowListener onShowListener;
    private OnDismissListener onDismissListener;

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

    public void setScannableCode(String scannableCode) {
        this.scannableCode = scannableCode;
    }

    private void lookupAndShowProduct(final ScannableCode scannedCode) {
        productConfirmationDialog.dismiss();
        progressDialog.showAfterDelay(300);

        if (onShowListener != null) {
            onShowListener.onShow();
        }

        ProductDatabase productDatabase = SnabbleUI.getProject().getProductDatabase();

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

    private void handleProductAvailable(Product product, boolean wasOnlineProduct, ScannableCode scannedCode) {
        progressDialog.dismiss();

        if(product.getBundleProducts().length > 0){
            showBundleDialog(product);
        } else {
            if (product.getType() == Product.Type.PreWeighed && !scannedCode.hasEmbeddedData()) {
                Toast.makeText(context,
                        R.string.Snabble_Scanner_scannedShelfCode,
                        Toast.LENGTH_LONG)
                        .show();

                progressDialog.dismiss();
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

                String[] codes = product.getScannableCodes();
                if(codes.length > 0) {
                    showProduct(product, ScannableCode.parse(SnabbleUI.getProject(), codes[0]));
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

        if(onDismissListener != null) {
            onDismissListener.onDismiss();
        }

        Telemetry.event(Telemetry.Event.ScannedUnknownCode, scannedCode.getCode());
        Toast.makeText(context,
                R.string.Snabble_Scanner_unknownBarcode,
                Toast.LENGTH_LONG)
                .show();
    }

    private void handleProductError() {
        progressDialog.dismiss();

        if(onDismissListener != null) {
            onDismissListener.onDismiss();
        }

        Toast.makeText(context,
                R.string.Snabble_Scanner_networkError,
                Toast.LENGTH_LONG)
                .show();
    }

    private void showProduct(Product product, ScannableCode scannedCode) {
        // TODO do i need this?
//        if (onShowListener != null) {
//            onShowListener.onShow();
//        }
//
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

    public static class Builder {
        private ProductResolver productResolver;

        public Builder(Context context) {
            productResolver = new ProductResolver(context);
        }

        public Builder setCode(String code) {
            productResolver.setScannableCode(code);
            return this;
        }

        public Builder setOnShowListener(OnShowListener listener) {
            productResolver.setOnShowListener(listener);
            return this;
        }

        public Builder setOnDismissListener(OnDismissListener listener) {
            productResolver.setOnDismissListener(listener);
            return this;
        }

        public void setSnackbarHost(View view) {
            // TODO howto snackbar
        }

        public ProductResolver create() {
            return productResolver;
        }
    }
}

package io.snabble.sdk.ui.scanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.math.RoundingMode;
import io.snabble.sdk.Product;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.PriceFormatter;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.InputFilterMinMax;

class ProductConfirmationDialog {
    private Context context;
    private AlertDialog alertDialog;
    private ShoppingCart shoppingCart;
    private PriceFormatter priceFormatter;

    private EditText quantity;
    private TextView subtitle;
    private TextView name;
    private TextView price;
    private TextView depositPrice;
    private TextView quantityAnnotation;
    private AppCompatButton addToCart;
    private View close;
    private View plus;
    private View minus;

    private Product product;
    private ScannableCode scannedCode;

    private DialogInterface.OnDismissListener onDismissListener;
    private DialogInterface.OnShowListener onShowListener;


    public ProductConfirmationDialog(Context context,
                                     SnabbleSdk sdkInstance) {
        this.context = context;
        this.shoppingCart = sdkInstance.getShoppingCart();
        priceFormatter = new PriceFormatter(sdkInstance);
    }

    public void show(Product newProduct, ScannableCode scannedCode) {
        dismiss();

        this.product = newProduct;
        this.scannedCode = scannedCode;

        View view = View.inflate(context, R.layout.dialog_product_confirmation, null);

        alertDialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        alertDialog.setOnShowListener(onShowListener);
        alertDialog.setOnDismissListener(onDismissListener);

        quantity = view.findViewById(R.id.quantity);
        subtitle = view.findViewById(R.id.subtitle);
        name = view.findViewById(R.id.name);
        price = view.findViewById(R.id.price);
        depositPrice = view.findViewById(R.id.depositPrice);
        quantityAnnotation = view.findViewById(R.id.quantity_annotation);
        addToCart = view.findViewById(R.id.addToCart);
        close = view.findViewById(R.id.close);
        plus = view.findViewById(R.id.plus);
        minus = view.findViewById(R.id.minus);

        name.setText(product.getName());

        if (product.getSubtitle() == null || product.getSubtitle().equals("")) {
            subtitle.setVisibility(View.GONE);
        } else {
            subtitle.setText(product.getSubtitle());
        }

        if (scannedCode.hasEmbeddedData() && scannedCode.getEmbeddedData() > 0) {
            quantity.setEnabled(false);
        }

        price.setVisibility(View.VISIBLE);
        quantity.clearFocus();

        Product.Type type = product.getType();

        int cartQuantity = shoppingCart.getQuantity(product);

        if (scannedCode.hasEmbeddedData()) {
            if(scannedCode.hasWeighData()) {
                quantityAnnotation.setText("g");
                plus.setVisibility(View.GONE);
                minus.setVisibility(View.GONE);
                quantityAnnotation.setVisibility(View.VISIBLE);
                quantity.setText(String.valueOf(scannedCode.getEmbeddedData()));
            } else if(scannedCode.hasPriceData()){
                quantityAnnotation.setText(SnabbleUI.getSdkInstance().getCurrency().getSymbol());
                plus.setVisibility(View.GONE);
                minus.setVisibility(View.GONE);
                quantityAnnotation.setVisibility(View.GONE);
                price.setVisibility(View.GONE);

                PriceFormatter priceFormatter = new PriceFormatter(SnabbleUI.getSdkInstance());
                quantity.setText(priceFormatter.format(scannedCode.getEmbeddedData()));
            } else if(scannedCode.hasUnitData()){
                if(scannedCode.getEmbeddedData() == 0) {
                    quantityAnnotation.setVisibility(View.GONE);
                    plus.setVisibility(View.VISIBLE);
                    minus.setVisibility(View.VISIBLE);
                    quantity.setText("1");
                } else {
                    quantityAnnotation.setVisibility(View.GONE);
                    plus.setVisibility(View.GONE);
                    minus.setVisibility(View.GONE);
                    quantity.setText(String.valueOf(scannedCode.getEmbeddedData()));
                }
            }
        } else if (type == Product.Type.Article) {
            quantityAnnotation.setVisibility(View.GONE);
            quantity.setText(String.valueOf(Math.min(ShoppingCart.MAX_QUANTITY, cartQuantity + 1)));
        } else if (type == Product.Type.UserWeighed) {
            quantityAnnotation.setVisibility(View.VISIBLE);
            quantityAnnotation.setText("g");
            plus.setVisibility(View.GONE);
            minus.setVisibility(View.GONE);
            quantity.setText("");
        }

        quantity.setFilters(new InputFilter[]{new InputFilterMinMax(1, ShoppingCart.MAX_QUANTITY)});
        quantity.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    addToCart();
                    return true;
                }

                return false;
            }
        });

        quantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // its possible that the callback gets called before a dismiss is dispatched
                // and when that happens the product is already null
                if (product == null) {
                    dismiss();
                    return;
                }

                 updatePrice();
            }
        });

        updatePrice();

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int q = getQuantity();
                if (q < ShoppingCart.MAX_QUANTITY) {
                    setQuantity(++q);
                }
            }
        });

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int q = getQuantity();
                setQuantity(--q);
            }
        });

        addToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToCart();
            }
        });

        if (cartQuantity > 0 && product.getType() == Product.Type.Article && !scannedCode.hasUnitData()) {
            addToCart.setText(R.string.Snabble_Scanner_updateCart);
        } else {
            addToCart.setText(R.string.Snabble_Scanner_addToCart);
        }

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Telemetry.event(Telemetry.Event.RejectedProduct, product);
                dismiss();
            }
        });

        Window window = alertDialog.getWindow();
        if (window == null) {
            product = null;
            return;
        }

        window.setGravity(Gravity.BOTTOM);
        alertDialog.show();

        if(product.getType() == Product.Type.UserWeighed){
            quantity.requestFocus();

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager inputMethodManager = (InputMethodManager)context
                            .getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(quantity, 0);
                }
            });
        }

        float density = context.getResources().getDisplayMetrics().density;
        window.setLayout(Math.round(336 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void updatePrice() {
        RoundingMode roundingMode = SnabbleUI.getSdkInstance().getRoundingMode();

        String priceText = priceFormatter.format(product.getPriceForQuantity(getQuantity(),
                roundingMode));
        String singlePrice = priceFormatter.format(product);

        int q = getQuantity();

        if(q > 0 && (scannedCode.hasWeighData() || product.getType() == Product.Type.UserWeighed)){
            price.setText(String.format("%sg * %s = %s", String.valueOf(q), singlePrice, priceText));
        } else if(q > 1){
            if(scannedCode.hasUnitData()){
                price.setText(String.format("%s * %s = %s",
                        String.valueOf(q),
                        priceFormatter.format(product.getPrice()),
                        priceFormatter.format(product.getPrice() * q)));
            } else {
                price.setText(String.format("%s * %s = %s", String.valueOf(q), singlePrice, priceText));
            }
        } else {
            price.setText(singlePrice);
        }

        Product depositProduct = product.getDepositProduct();
        if(depositProduct != null){
            String depositPriceText = priceFormatter.format(depositProduct.getPriceForQuantity(getQuantity(),
                    roundingMode));

            Resources res = context.getResources();
            String text = res.getString(R.string.Snabble_Scanner_plusDeposit, depositPriceText);
            depositPrice.setText(text);
            depositPrice.setVisibility(View.VISIBLE);
        } else {
            depositPrice.setVisibility(View.GONE);
        }
    }

    private void addToCart() {
        // its possible that the onClickListener gets called before a dismiss is dispatched
        // and when that happens the product is already null
        if(product == null){
            dismiss();
            return;
        }

        Telemetry.event(Telemetry.Event.ConfirmedProduct, product);

        int q = getQuantity();

        if(scannedCode.hasEmbeddedData()){
            boolean isZeroAmountProduct = false;

            // generate new code when the embedded data contains 0
            if (scannedCode.getEmbeddedData() == 0) {
                scannedCode = EAN13.generateNewCodeWithEmbeddedData(SnabbleUI.getSdkInstance(),
                        scannedCode.getCode(), getQuantity());
                isZeroAmountProduct = true;
            }

            // if the user entered 0, shake
            if(scannedCode.getEmbeddedData() == 0) {
                shake();
                return;
            } else {
                shoppingCart.add(product, 1, scannedCode, isZeroAmountProduct);
            }
        } else if (product.getType() == Product.Type.Article) {
            shoppingCart.setQuantity(product, q, scannedCode);
        } else if(product.getType() == Product.Type.UserWeighed){
            if(q > 0) {
                shoppingCart.add(product, q, scannedCode);
            } else {
                shake();
                return;
            }
        }

        dismiss();
    }

    private void shake() {
        float density =  context.getResources().getDisplayMetrics().density;
        TranslateAnimation shake = new TranslateAnimation(0, 3 * density, 0, 0);
        shake.setDuration(500);
        shake.setInterpolator(new CycleInterpolator(7));
        quantity.startAnimation(shake);
    }

    private int getQuantity() {
        try {
            return Integer.parseInt(quantity.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setQuantity(int number) {
        // its possible that the onClickListener gets called before a dismiss is dispatched
        // and when that happens the product is already null
        if(product == null){
            dismiss();
            return;
        }

        quantity.setText(String.valueOf(number));
        quantity.setSelection(quantity.getText().length());
    }

    public void dismiss() {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog.setOnDismissListener(null);
            alertDialog = null;
        }

        product = null;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public void setOnShowListener(DialogInterface.OnShowListener onShowListener){
        this.onShowListener = onShowListener;
    }
}

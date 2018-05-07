package io.snabble.sdk.ui.scanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Product;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.PriceFormatter;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.InputFilterMinMax;
import io.snabble.sdk.ui.utils.OneShotClickListener;

class ProductConfirmationDialog {
    private Context context;
    private AlertDialog alertDialog;
    private ShoppingCart shoppingCart;
    private Checkout checkout;
    private PriceFormatter priceFormatter;

    private EditText quantity;
    private TextView subtitle;
    private TextView name;
    private TextView price;
    private TextView quantityAnnotation;
    private AppCompatButton addToCart;
    private View close;
    private View plus;
    private View minus;
    private TextView payNow;

    private Product product;
    private ScannableCode scannedCode;

    private DialogInterface.OnDismissListener onDismissListener;
    private DialogInterface.OnShowListener onShowListener;
    private String payNowText;

    public ProductConfirmationDialog(Context context,
                                     SnabbleSdk sdkInstance) {
        this.context = context;
        this.shoppingCart = sdkInstance.getShoppingCart();
        this.checkout = sdkInstance.getCheckout();
        priceFormatter = new PriceFormatter(sdkInstance);

        payNowText = context.getString(R.string.Snabble_Scanner_gotoCheckout);
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
        quantityAnnotation = view.findViewById(R.id.quantity_annotation);
        addToCart = view.findViewById(R.id.addToCart);
        close = view.findViewById(R.id.close);
        plus = view.findViewById(R.id.plus);
        minus = view.findViewById(R.id.minus);
        payNow = view.findViewById(R.id.pay_now);

        payNow.setText(payNowText);

        name.setText(product.getName());
        price.setText(priceFormatter.format(product));

        if (product.getSubtitle() == null || product.getSubtitle().equals("")) {
            subtitle.setVisibility(View.GONE);
        } else {
            subtitle.setText(product.getSubtitle());
        }

        Product.Type type = product.getType();
        if (type == Product.Type.UserWeighed || type == Product.Type.PreWeighed) {
            quantityAnnotation.setText("g");
            plus.setVisibility(View.GONE);
            minus.setVisibility(View.GONE);
            quantityAnnotation.setVisibility(View.VISIBLE);
        } else {
            quantityAnnotation.setVisibility(View.GONE);
        }

        if (type == Product.Type.PreWeighed) {
            quantity.setEnabled(false);
        }

        price.setVisibility(View.VISIBLE);

        if(scannedCode.hasWeighData()) {
            quantityAnnotation.setText("g");
            plus.setVisibility(View.GONE);
            minus.setVisibility(View.GONE);
            quantityAnnotation.setVisibility(View.VISIBLE);
        } else if(scannedCode.hasPriceData()){
            quantityAnnotation.setText(SnabbleUI.getSdkInstance().getCurrency().getSymbol());
            plus.setVisibility(View.GONE);
            minus.setVisibility(View.GONE);
            quantityAnnotation.setVisibility(View.GONE);
            price.setVisibility(View.GONE);
        } else if(scannedCode.hasAmountData()){
            quantityAnnotation.setVisibility(View.GONE);
            plus.setVisibility(View.GONE);
            minus.setVisibility(View.GONE);
        } else {
            quantityAnnotation.setVisibility(View.GONE);
        }

        quantity.clearFocus();

        int cartQuantity = shoppingCart.getQuantity(product);

        if (type == Product.Type.Article) {
            quantityAnnotation.setVisibility(View.GONE);
            quantity.setText(String.valueOf(Math.min(ShoppingCart.MAX_QUANTITY, cartQuantity + 1)));
        } else if (type == Product.Type.UserWeighed) {
            quantityAnnotation.setVisibility(View.VISIBLE);
            quantityAnnotation.setText("g");
            quantity.setText(""); // initial value ?
        } else if (type == Product.Type.PreWeighed) {
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
            } else if(scannedCode.hasAmountData()){
                quantityAnnotation.setVisibility(View.GONE);
                plus.setVisibility(View.GONE);
                minus.setVisibility(View.GONE);
                quantity.setText(String.valueOf(scannedCode.getEmbeddedData()));
            }
        }

        if(type != Product.Type.PreWeighed) {
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

                    updatePayText();
                }
            });
        }

        updatePayText();

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

        if (cartQuantity > 0) {
            addToCart.setText(R.string.Snabble_Scanner_addToCart);
        } else {
            addToCart.setText(R.string.Snabble_Scanner_updateCart);
        }

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Telemetry.event(Telemetry.Event.RejectedProduct, product);
                dismiss();
            }
        });

        payNow.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                addToCart();
                checkout.checkout();
            }
        });

        Window window = alertDialog.getWindow();
        if (window == null) {
            product = null;
            return;
        }

        window.setGravity(Gravity.BOTTOM);
        alertDialog.show();

        float density = context.getResources().getDisplayMetrics().density;
        window.setLayout(Math.round(336 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
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

        if (product.getType() == Product.Type.Article) {
            shoppingCart.setQuantity(product, q, scannedCode);
        } else if(product.getType() == Product.Type.PreWeighed){
            shoppingCart.add(product, 1, scannedCode);
        } else if(product.getType() == Product.Type.UserWeighed){
            if(q > 0) {
                shoppingCart.add(product, q, scannedCode);
            } else {
                TranslateAnimation shake = new TranslateAnimation(0, 10, 0, 0);
                shake.setDuration(500);
                shake.setInterpolator(new CycleInterpolator(7));
                quantity.startAnimation(shake);
                return;
            }
        }

        dismiss();
    }

    private void updatePayText() {
        if (checkout.isAvailable()) {
            int totalPrice = shoppingCart.getTotalPrice();
            totalPrice -= product.getPriceForQuantity(shoppingCart.getQuantity(product));
            totalPrice += product.getPriceForQuantity(getQuantity());
            String formattedTotalPrice = priceFormatter.format(totalPrice);
            payNow.setVisibility(totalPrice > 0 ? View.VISIBLE : View.INVISIBLE);
            payNow.setText(String.format(Locale.getDefault(), payNowText, formattedTotalPrice));
        } else {
            payNow.setVisibility(View.GONE);
        }
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

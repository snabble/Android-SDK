package io.snabble.sdk.ui.scanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.widget.AppCompatButton;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
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
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.ShoppingCart2;
import io.snabble.sdk.Unit;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.InputFilterMinMax;

class ProductConfirmationDialog {
    private Context context;
    private AlertDialog alertDialog;
    private ShoppingCart2 shoppingCart;
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
    private ScannedCode scannedCode;

    private DialogInterface.OnDismissListener onDismissListener;
    private DialogInterface.OnShowListener onShowListener;


    public ProductConfirmationDialog(Context context,
                                     Project project) {
        this.context = context;
        this.shoppingCart = project.getShoppingCart();
        priceFormatter = new PriceFormatter(project);
    }

    public void show(Product newProduct, ScannedCode scannedCode) {
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
        int cartQuantity = 1; // TODO get current item

        Unit unit = product.getEncodingUnit(scannedCode.getTemplateName(), scannedCode.getLookupCode());
        if (scannedCode.getEmbeddedUnit() != null) {
            unit = scannedCode.getEmbeddedUnit();
        }

        int productPrice = product.getDiscountedPrice();
        if (scannedCode.hasPrice()) {
            productPrice = scannedCode.getPrice();
        }

        if (scannedCode.hasEmbeddedData()) {
            if (Unit.hasDimension(unit)) {
                quantityAnnotation.setText(getNonNullEncodingUnit(product, scannedCode).getDisplayValue());
                quantityAnnotation.setVisibility(View.VISIBLE);
                plus.setVisibility(View.GONE);
                minus.setVisibility(View.GONE);
                quantity.setText(String.valueOf(scannedCode.getEmbeddedData()));
            } else if (unit == Unit.PRICE) {
                quantityAnnotation.setText(SnabbleUI.getProject().getCurrency().getSymbol());
                plus.setVisibility(View.GONE);
                minus.setVisibility(View.GONE);
                quantityAnnotation.setVisibility(View.GONE);
                price.setVisibility(View.GONE);

                PriceFormatter priceFormatter = new PriceFormatter(SnabbleUI.getProject());
                quantity.setText(priceFormatter.format(scannedCode.getEmbeddedData()));
            } else if (unit == Unit.PIECE) {
                if (scannedCode.getEmbeddedData() == 0) {
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

            if (product.getReferenceUnit() == Unit.PIECE) {
                quantity.setText("1");
            } else {
                quantity.setText(String.valueOf(Math.min(ShoppingCart.MAX_QUANTITY, cartQuantity + 1)));
            }

            // special case if price is zero we assume its a picking product and hide the
            // controls to adjust the quantity
            if (productPrice == 0) {
                plus.setVisibility(View.GONE);
                minus.setVisibility(View.GONE);
                quantity.setEnabled(false);
                quantity.setText("1");
            }
        } else if (type == Product.Type.UserWeighed) {
            quantityAnnotation.setVisibility(View.VISIBLE);
            quantityAnnotation.setText(getNonNullEncodingUnit(product, scannedCode).getDisplayValue());
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

        if (cartQuantity > 0
                && product.getType() == Product.Type.Article
                && unit != Unit.PRICE
                && unit != Unit.PIECE
                && product.getDiscountedPrice() != 0) {
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

        if (product.getType() == Product.Type.UserWeighed) {
            quantity.requestFocus();

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager inputMethodManager = (InputMethodManager) context
                            .getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(quantity, 0);
                }
            });
        }

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int availableWidth = Math.round(dm.widthPixels / dm.density);
        int width = Math.round(320 * dm.density);
        if(availableWidth >= 336) {
            width = Math.round(336 * dm.density);
        }

        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private Unit getNonNullEncodingUnit(Product product, ScannedCode code) {
        if (code.getEmbeddedUnit() != null) {
            return code.getEmbeddedUnit();
        }

        Unit unit = product.getEncodingUnit(code.getTemplateName(), code.getLookupCode());
        if (unit == null) {
            unit = Unit.GRAM;
        }

        return unit;
    }

    private void updatePrice() {
        RoundingMode roundingMode = SnabbleUI.getProject().getRoundingMode();

        String priceText = priceFormatter.format(product.getPriceForQuantity(getQuantity(), scannedCode, roundingMode));
        String singlePrice = priceFormatter.format(product, true, scannedCode);

        int q = getQuantity();
        Unit encodingUnit = product.getEncodingUnit(scannedCode.getTemplateName(), scannedCode.getCode());

        if (scannedCode.getEmbeddedUnit() != null) {
            encodingUnit = scannedCode.getEmbeddedUnit();
        }

        String encodingDisplayValue = "g";
        if (encodingUnit != null) {
            encodingDisplayValue = encodingUnit.getDisplayValue();
        }

        int productPrice = product.getDiscountedPrice();
        if (scannedCode.hasPrice()) {
            productPrice = scannedCode.getPrice();
        }

        if (q > 0 && (Unit.hasDimension(encodingUnit) || product.getType() == Product.Type.UserWeighed)) {
            price.setText(String.format("%s %s * %s = %s", String.valueOf(q), encodingDisplayValue, singlePrice, priceText));
        } else if (q > 1) {
            if (encodingUnit == Unit.PIECE) {
                price.setText(String.format("%s * %s = %s",
                        String.valueOf(q),
                        priceFormatter.format(productPrice),
                        priceFormatter.format(productPrice * q)));
            } else {
                price.setText(String.format("%s * %s = %s", String.valueOf(q), singlePrice, priceText));
            }
        } else {
            price.setText(singlePrice);
        }

        if (encodingUnit == Unit.PRICE) {
            int embeddedPrice = scannedCode.getEmbeddedData();
            price.setText(priceFormatter.format(embeddedPrice));
        }

        Product depositProduct = product.getDepositProduct();
        if (depositProduct != null) {
            String depositPriceText = priceFormatter.format(depositProduct.getPriceForQuantity(getQuantity(), scannedCode, roundingMode));
            Resources res = context.getResources();
            String text = res.getString(R.string.Snabble_Scanner_plusDeposit, depositPriceText);
            depositPrice.setText(text);
            depositPrice.setVisibility(View.VISIBLE);
        } else {
            depositPrice.setVisibility(View.GONE);
        }

        if (price.getText().equals("")) {
            price.setVisibility(View.GONE);
        } else {
            price.setVisibility(View.VISIBLE);
        }
    }

    private void addToCart() {
        // its possible that the onClickListener gets called before a dismiss is dispatched
        // and when that happens the product is already null
        if (product == null) {
            dismiss();
            return;
        }

        Telemetry.event(Telemetry.Event.ConfirmedProduct, product);

        int q = getQuantity();
        shoppingCart.add(product, scannedCode).setQuantity(q);

        dismiss();
    }

    private void shake() {
        float density = context.getResources().getDisplayMetrics().density;
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
        if (product == null) {
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

    public void setOnShowListener(DialogInterface.OnShowListener onShowListener) {
        this.onShowListener = onShowListener;
    }
}

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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
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
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Unit;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.ui.R;
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
    private TextView originalPrice;
    private TextView depositPrice;
    private TextView quantityAnnotation;
    private AppCompatButton addToCart;
    private View close;
    private View plus;
    private View minus;

    private ShoppingCart.Item cartItem;

    private DialogInterface.OnDismissListener onDismissListener;
    private DialogInterface.OnShowListener onShowListener;


    public ProductConfirmationDialog(Context context,
                                     Project project) {
        this.context = context;
        this.shoppingCart = project.getShoppingCart();
        this.priceFormatter = project.getPriceFormatter();
    }

    public void show(Product product, ScannedCode scannedCode) {
        dismiss();

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
        originalPrice = view.findViewById(R.id.originalPrice);
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

        cartItem = shoppingCart.newItem(product, scannedCode);

        ShoppingCart.Item existingItem = shoppingCart.getByProduct(product);
        if (existingItem != null && existingItem.isMergeable()) {
            setQuantity(existingItem.getEffectiveQuantity() + 1);
        } else {
            setQuantity(cartItem.getEffectiveQuantity());
        }

        Unit unit = cartItem.getUnit();
        if (unit != null) {
            quantityAnnotation.setText(unit.getDisplayValue());
            quantityAnnotation.setVisibility(View.VISIBLE);
        } else {
            quantityAnnotation.setVisibility(View.GONE);
        }

        if (existingItem != null && existingItem.isMergeable()) {
            addToCart.setText(R.string.Snabble_Scanner_updateCart);
        } else {
            addToCart.setText(R.string.Snabble_Scanner_addToCart);
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
                if (cartItem == null) {
                    dismiss();
                    return;
                }

                int number;

                try {
                    number = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    number = 0;
                }

                cartItem.setQuantity(number);
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

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Telemetry.event(Telemetry.Event.RejectedProduct, cartItem.getProduct());
                dismiss();
            }
        });

        Window window = alertDialog.getWindow();
        if (window == null) {
            cartItem = null;
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

    private void updatePrice() {
        Product product = cartItem.getProduct();

        String fullPriceText = cartItem.getFullPriceText();
        if (fullPriceText != null) {
            price.setText(cartItem.getFullPriceText());
            price.setVisibility(View.VISIBLE);

            if (product.getPrice() > product.getDiscountedPrice()) {
                Spannable originalPriceText = new SpannableString(priceFormatter.format(product.getPrice()));
                originalPriceText.setSpan(new StrikethroughSpan(), 0, originalPriceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                originalPrice.setVisibility(View.VISIBLE);
                originalPrice.setText(originalPriceText);
            } else {
                originalPrice.setVisibility(View.GONE);
            }
        } else {
            price.setVisibility(View.GONE);
        }

        if (cartItem.getUnit() == Unit.PRICE) {
            price.setVisibility(View.GONE);
        }

        int cartItemDepositPrice = cartItem.getTotalDepositPrice();
        if (cartItemDepositPrice == 0 && cartItem.getProduct().getDepositProduct() != null) {
            cartItemDepositPrice = cartItem.getProduct().getDepositProduct().getDiscountedPrice();
        }

        if (cartItemDepositPrice > 0) {
            String depositPriceText = priceFormatter.format(cartItemDepositPrice);
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
        if (cartItem == null) {
            dismiss();
            return;
        }

        Telemetry.event(Telemetry.Event.ConfirmedProduct, cartItem.getProduct());

        int q = getQuantity();
        if (cartItem.getProduct().getType() == Product.Type.UserWeighed && q == 0) {
            shake();
            return;
        }

        if (shoppingCart.indexOf(cartItem) == -1) {
            shoppingCart.add(cartItem);
        }

        cartItem.setQuantity(q);

        shoppingCart.updatePrices(false);

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
        if (cartItem == null) {
            dismiss();
            return;
        }

        if (cartItem.isEditable()) {
            quantity.setEnabled(true);

            if (cartItem.getProduct().getType() == Product.Type.UserWeighed) {
                plus.setVisibility(View.GONE);
                minus.setVisibility(View.GONE);
            } else {
                plus.setVisibility(View.VISIBLE);
                minus.setVisibility(View.VISIBLE);
            }
        } else {
            quantity.setEnabled(false);
            plus.setVisibility(View.GONE);
            minus.setVisibility(View.GONE);
        }

        if (cartItem.getProduct().getType() != Product.Type.UserWeighed) {
            quantity.setText(String.valueOf(number));
            quantity.setSelection(quantity.getText().length());
        } else {
            quantity.setText("");
        }

        if (cartItem.getUnit() == Unit.PRICE) {
            quantity.setText(cartItem.getPriceText());
        }

        cartItem.setQuantity(number);
        updatePrice();
    }

    public void dismiss() {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog.setOnDismissListener(null);
            alertDialog = null;
        }

        cartItem = null;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public void setOnShowListener(DialogInterface.OnShowListener onShowListener) {
        this.onShowListener = onShowListener;
    }
}

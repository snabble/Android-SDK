package io.snabble.sdk.ui.scanner;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.squareup.picasso.Picasso;

import java.util.List;

import io.snabble.sdk.Coupon;
import io.snabble.sdk.CouponType;
import io.snabble.sdk.PriceFormatter;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.Unit;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.InputFilterMinMax;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.GsonHolder;

public class ProductConfirmationDialog {
    private Context context;
    private Project project;
    private AlertDialog alertDialog;
    private ShoppingCart shoppingCart;
    private PriceFormatter priceFormatter;

    private EditText quantity;
    private View quantityTextInput;
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
    private Button enterReducedPrice;

    private ShoppingCart.Item cartItem;

    private DialogInterface.OnDismissListener onDismissListener;
    private DialogInterface.OnShowListener onShowListener;
    private DialogInterface.OnKeyListener onKeyListener;
    private boolean wasAddedToCart;

    public ProductConfirmationDialog(Context context,
                                     Project project) {
        this.context = context;
        this.shoppingCart = project.getShoppingCart();
        this.project = project;
        this.priceFormatter = project.getPriceFormatter();
    }

    public void show(Product product, ScannedCode scannedCode) {
        dismiss(false);

        View view = View.inflate(context, R.layout.snabble_dialog_product_confirmation, null);

        alertDialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        alertDialog.setOnShowListener(onShowListener);
        alertDialog.setOnDismissListener(onDismissListener);
        alertDialog.setOnKeyListener(onKeyListener);

        quantity = view.findViewById(R.id.quantity);
        quantityTextInput = view.findViewById(R.id.quantity_text_input);
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
        enterReducedPrice = view.findViewById(R.id.enterReducedPrice);

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

        Unit unit = cartItem.getUnit();
        if (unit != null) {
            quantityAnnotation.setText(unit.getDisplayValue());
            quantityAnnotation.setVisibility(View.VISIBLE);
        } else {
            quantityAnnotation.setVisibility(View.GONE);
        }

        updateQuantityText();

        quantity.setFilters(new InputFilter[]{new InputFilterMinMax(1, ShoppingCart.MAX_QUANTITY)});
        quantity.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                addToCart();
                return true;
            }

            return false;
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
                    dismiss(false);
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

        plus.setOnClickListener(v -> {
            int q = getQuantity();
            if (q < ShoppingCart.MAX_QUANTITY) {
                setQuantity(++q);
            }
        });

        minus.setOnClickListener(v -> {
            int q = getQuantity();
            setQuantity(--q);
        });

        addToCart.setOnClickListener(v -> addToCart());

        close.setOnClickListener(v -> {
            Telemetry.event(Telemetry.Event.RejectedProduct, cartItem.getProduct());
            cartItem = null;
            dismiss(false);
        });

        Window window = alertDialog.getWindow();
        if (window == null) {
            cartItem = null;
            return;
        }

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int marginBottom = Math.round(48 * dm.density);

        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.y = marginBottom;
        window.setBackgroundDrawableResource(R.drawable.snabble_scanner_dialog_background);
        window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        window.setAttributes(layoutParams);
        alertDialog.show();

        if (product.getType() == Product.Type.UserWeighed) {
            quantity.requestFocus();

            Dispatch.mainThread(() -> {
                InputMethodManager inputMethodManager = (InputMethodManager) context
                        .getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(quantity, 0);
            });
        }

        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            callback.execute(SnabbleUI.Action.EVENT_PRODUCT_CONFIRMATION_SHOW, null);
        }
    }

    public void updateQuantityText() {
        ShoppingCart.Item existingItem = shoppingCart.getExistingMergeableProduct(cartItem.getProduct());
        boolean isMergeable = existingItem != null && existingItem.isMergeable() && cartItem.isMergeable();
        if (isMergeable) {
            setQuantity(existingItem.getEffectiveQuantity() + 1);
            addToCart.setText(R.string.Snabble_Scanner_updateCart);
        } else {
            setQuantity(cartItem.getEffectiveQuantity());
            addToCart.setText(R.string.Snabble_Scanner_addToCart);
        }
    }

    public void updatePrice() {
        Product product = cartItem.getProduct();

        String fullPriceText = cartItem.getFullPriceText();
        if (fullPriceText != null) {
            price.setText(cartItem.getFullPriceText());
            price.setVisibility(View.VISIBLE);

            if (product.getListPrice() > product.getPrice(project.getCustomerCardId())) {
                Spannable originalPriceText = new SpannableString(priceFormatter.format(product.getListPrice()));
                originalPriceText.setSpan(new StrikethroughSpan(), 0, originalPriceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                originalPrice.setVisibility(View.VISIBLE);
                originalPrice.setText(originalPriceText);
            } else {
                originalPrice.setVisibility(View.GONE);
            }
        } else {
            price.setVisibility(View.GONE);
            originalPrice.setVisibility(View.GONE);
        }

        int cartItemDepositPrice = cartItem.getTotalDepositPrice();
        if (cartItemDepositPrice == 0 && cartItem.getProduct().getDepositProduct() != null) {
            cartItemDepositPrice = cartItem.getProduct().getDepositProduct().getPrice(project.getCustomerCardId());
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

        List<Coupon> manualCoupons = project.getCoupons().get(CouponType.MANUAL);
        boolean isVisible = manualCoupons != null && manualCoupons.size() > 0;
        enterReducedPrice.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        enterReducedPrice.setOnClickListener(v -> {
            FragmentActivity fragmentActivity = UIUtils.getHostFragmentActivity(context);
            new SelectReducedPriceDialogFragment(ProductConfirmationDialog.this, cartItem, shoppingCart)
                    .show(fragmentActivity.getSupportFragmentManager(), null);
        });

        if (cartItem.getCoupon() != null) {
            enterReducedPrice.setText(cartItem.getCoupon().getName());
        } else {
            enterReducedPrice.setText(R.string.Snabble_addDiscount);
        }
    }

    public void addToCart() {
        // its possible that the onClickListener gets called before a dismiss is dispatched
        // and when that happens the product is already null
        if (cartItem == null) {
            dismiss(false);
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

        // warm up the image cache
        String imageUrl = cartItem.getProduct().getImageUrl();
        if (imageUrl != null && imageUrl.length() > 0) {
            Picasso.get().load(cartItem.getProduct().getImageUrl()).fetch();
        }

        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            Bundle args = new Bundle();
            args.putString("cartItem", GsonHolder.get().toJson(cartItem));
            callback.execute(SnabbleUI.Action.EVENT_PRODUCT_CONFIRMATION_HIDE, args);
        }
        dismiss(true);

        if (Snabble.getInstance().getConfig().vibrateToConfirmCartFilled &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.VIBRATE)
                        == PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            // noinspection MissingPermission, check is above
            vibrator.vibrate(200L);
        }
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

    public void setQuantity(int number) {
        // its possible that the onClickListener gets called before a dismiss is dispatched
        // and when that happens the product is already null
        if (cartItem == null) {
            dismiss(false);
            return;
        }

        if (cartItem.isEditableInDialog()) {
            quantity.setEnabled(true);

            if (cartItem.getProduct().getType() == Product.Type.UserWeighed) {
                plus.setVisibility(View.GONE);
                minus.setVisibility(View.GONE);
            } else {
                plus.setVisibility(View.VISIBLE);
                minus.setVisibility(View.VISIBLE);
                quantity.setVisibility(View.VISIBLE);
                quantityTextInput.setVisibility(View.VISIBLE);
            }
        } else {
            quantity.setEnabled(false);
            plus.setVisibility(View.GONE);
            minus.setVisibility(View.GONE);
            quantity.setVisibility(View.GONE);
            quantityTextInput.setVisibility(View.GONE);
            quantityAnnotation.setVisibility(View.GONE);
        }

        if (cartItem.getProduct().getType() != Product.Type.UserWeighed) {
            if (cartItem.getProduct().getType() == Product.Type.Article) {
                number = Math.max(1, number);
            }

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

    public void dismiss(boolean addToCart) {
        wasAddedToCart = addToCart;

        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog.setOnDismissListener(null);
            alertDialog = null;

            if (!addToCart) {
                SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.execute(SnabbleUI.Action.EVENT_PRODUCT_CONFIRMATION_HIDE, null);
                }
            }
        }

        cartItem = null;
    }

    public boolean wasAddedToCart() {
        return wasAddedToCart;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public void setOnShowListener(DialogInterface.OnShowListener onShowListener) {
        this.onShowListener = onShowListener;
    }

    public void setOnKeyListener(DialogInterface.OnKeyListener onKeyListener) {
        this.onKeyListener = onKeyListener;
    }
}

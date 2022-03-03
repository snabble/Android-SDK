package io.snabble.sdk.ui.cart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import io.snabble.sdk.Product;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Unit;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.InputFilterMinMax;
import io.snabble.sdk.ui.utils.OneShotClickListener;

public class ShoppingCartItemViewHolder extends RecyclerView.ViewHolder {
    ImageView image;
    TextView name;
    TextView subtitle;
    TextView quantityTextView;
    TextView priceTextView;
    View plus;
    View minus;
    EditText quantityEdit;
    View controlsUserWeighed;
    View controlsDefault;
    View quantityEditApply;
    View quantityEditApplyLayout;
    TextView quantityAnnotation;
    TextView redLabel;
    TextWatcher textWatcher;
    private final UndoHelper undoHelper;
    private final Picasso picasso;

    ShoppingCartItemViewHolder(View itemView, UndoHelper undoHelper) {
        super(itemView);
        this.undoHelper = undoHelper;
        this.picasso = Picasso.get();

        image = itemView.findViewById(R.id.helper_image);
        name = itemView.findViewById(R.id.name);
        subtitle = itemView.findViewById(R.id.subtitle);
        quantityTextView = itemView.findViewById(R.id.quantity);
        priceTextView = itemView.findViewById(R.id.price);
        plus = itemView.findViewById(R.id.plus);
        minus = itemView.findViewById(R.id.minus);
        controlsUserWeighed = itemView.findViewById(R.id.controls_user_weighed);
        controlsDefault = itemView.findViewById(R.id.controls_default);
        quantityEdit = itemView.findViewById(R.id.quantity_edit);
        quantityEditApply = itemView.findViewById(R.id.quantity_edit_apply);
        quantityEditApplyLayout = itemView.findViewById(R.id.quantity_edit_apply_layout);
        quantityAnnotation = itemView.findViewById(R.id.quantity_annotation);
        redLabel = itemView.findViewById(R.id.red_label);
    }

    private static void setTextOrHide(TextView textView, String text) {
        if (text != null) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    public void bindTo(final ShoppingCartView.ProductRow row, boolean hasAnyImages) {
        setTextOrHide(name, row.name);
        setTextOrHide(priceTextView, row.priceText);
        setTextOrHide(quantityTextView, row.quantityText);

        if (row.imageUrl != null) {
            image.setVisibility(View.VISIBLE);
            picasso.load(row.imageUrl).into(image);
        } else {
            image.setVisibility(hasAnyImages ? View.INVISIBLE : View.GONE);
            image.setImageBitmap(null);
        }

        boolean hasCoupon = row.item.getCoupon() != null;
        boolean isAgeRestricted = false;

        redLabel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff0000")));

        if (row.item.getProduct() != null) {
            isAgeRestricted = row.item.getProduct().getSaleRestriction().isAgeRestriction();
        }

        redLabel.setVisibility(hasCoupon || isAgeRestricted ? View.VISIBLE : View.GONE);

        if (hasCoupon) {
            if (!row.manualDiscountApplied) {
                redLabel.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#999999")));
            }

            redLabel.setText("%");
        } else {
            long age = row.item.getProduct().getSaleRestriction().getValue();

            if (age > 0) {
                redLabel.setText(String.valueOf(age));
            } else {
                redLabel.setVisibility(View.GONE);
            }
        }

        String encodingDisplayValue = "g";
        Unit encodingUnit = row.encodingUnit;
        if (encodingUnit != null) {
            encodingDisplayValue = encodingUnit.getDisplayValue();
        }
        quantityAnnotation.setText(encodingDisplayValue);

        if (row.editable) {
            if (row.item.getProduct().getType() == Product.Type.UserWeighed) {
                controlsDefault.setVisibility(View.GONE);
                controlsUserWeighed.setVisibility(View.VISIBLE);
            } else {
                controlsDefault.setVisibility(View.VISIBLE);
                controlsUserWeighed.setVisibility(View.GONE);
            }
        } else {
            controlsDefault.setVisibility(View.GONE);
            controlsUserWeighed.setVisibility(View.GONE);
        }

        plus.setOnClickListener(v -> {
            row.item.setQuantity(row.item.getQuantity() + 1);
            Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.getProduct());
        });

        minus.setOnClickListener(v -> {
            int p = getBindingAdapterPosition();

            int newQuantity = row.item.getQuantity() - 1;
            if (newQuantity <= 0) {
                undoHelper.removeAndShowUndoSnackbar(p, row.item);
            } else {
                row.item.setQuantity(newQuantity);
                Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.getProduct());
            }
        });

        quantityEditApply.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                row.item.setQuantity(getQuantityEditValue());
                hideInput();
                Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.getProduct());
            }
        });

        quantityEdit.setText(Integer.toString(row.quantity));
        itemView.setFocusable(true);
        itemView.setFocusableInTouchMode(true);

        if (getBindingAdapterPosition() == 0) {
            itemView.requestFocus();
        }

        quantityEdit.removeTextChangedListener(textWatcher);
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateQuantityEditApplyVisibility(row.quantity);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        updateQuantityEditApplyVisibility(row.quantity);

        quantityEdit.addTextChangedListener(textWatcher);
        quantityEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                quantityEditApply.callOnClick();
                return true;
            }

            return false;
        });

        quantityEdit.setFilters(new InputFilter[]{new InputFilterMinMax(0, ShoppingCart.MAX_QUANTITY)});
    }

    void hideInput() {
        InputMethodManager imm = (InputMethodManager) quantityEdit.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(quantityEdit.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }

        quantityEdit.clearFocus();
    }

    private void updateQuantityEditApplyVisibility(int quantity) {
        int value = getQuantityEditValue();
        if (value > 0 && value != quantity) {
            quantityEditApply.setVisibility(View.VISIBLE);
            quantityEditApplyLayout.setVisibility(View.VISIBLE);
        } else {
            quantityEditApply.setVisibility(View.GONE);
            quantityEditApplyLayout.setVisibility(View.GONE);
        }
    }

    public int getQuantityEditValue() {
        try {
            return Integer.parseInt(quantityEdit.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

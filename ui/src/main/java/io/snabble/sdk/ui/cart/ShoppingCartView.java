package io.snabble.sdk.ui.cart;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.math.RoundingMode;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.codes.EAN13;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.PriceFormatter;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class ShoppingCartView extends FrameLayout implements Checkout.OnCheckoutStateChangedListener {
    private RecyclerView recyclerView;
    private Adapter recyclerViewAdapter;
    private ShoppingCart cart;
    private Checkout checkout;
    private PriceFormatter priceFormatter;
    private Button pay;
    private View coordinatorLayout;
    private View emptyState;
    private Snackbar snackbar;
    private DelayedProgressDialog progressDialog;
    private boolean hasAnyImages;

    private ShoppingCart.ShoppingCartListener shoppingCartListener = new ShoppingCart.ShoppingCartListener() {
        @Override
        public void onItemAdded(ShoppingCart list, Product product) {
            onCartUpdated();
        }

        @Override
        public void onQuantityChanged(ShoppingCart list, Product product) {
            
        }

        @Override
        public void onCleared(ShoppingCart list) {
            onCartUpdated();
        }

        @Override
        public void onItemMoved(ShoppingCart list, int fromIndex, int toIndex) {

        }

        @Override
        public void onItemRemoved(ShoppingCart list, Product product) {
            onCartUpdated();

            if (snackbar != null) {
                snackbar.show();
            }
        }
    };

    public ShoppingCartView(Context context) {
        super(context);
        inflateView(context);
    }

    public ShoppingCartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView(context);
    }

    public ShoppingCartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView(context);
    }

    private void inflateView(Context context) {
        inflate(getContext(), R.layout.view_shopping_cart, this);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerViewAdapter = new Adapter();
        recyclerView.setAdapter(recyclerViewAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        DividerItemDecoration itemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        int dividerColor = ResourcesCompat.getColor(getResources(), R.color.snabble_dividerColor, null);
        itemDecoration.setDrawable(new ColorDrawable(dividerColor));
        recyclerView.addItemDecoration(itemDecoration);

        coordinatorLayout = findViewById(R.id.coordinator_layout);
        emptyState = findViewById(R.id.empty_state);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_pleaseWait));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    checkout.cancel();
                    return true;
                }
                return false;
            }
        });

        pay = findViewById(R.id.pay);
        pay.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                checkout.checkout();
                Telemetry.event(Telemetry.Event.ClickCheckout);
            }
        });

        View scanProducts = findViewById(R.id.scan_products);
        scanProducts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showScannerWithCode(null);
                }
            }
        });

        createItemTouchHelper();

        Project project = SnabbleUI.getProject();

        if (cart != null) {
            cart.removeListener(shoppingCartListener);
        }

        cart = project.getShoppingCart();
        checkout = project.getCheckout();
        priceFormatter = new PriceFormatter(project);

        updatePayText();
        updateEmptyState();
        scanForImages();

        if (checkout.getState() != Checkout.State.CONNECTION_ERROR) {
            onStateChanged(checkout.getState());
        }
    }

    private void createItemTouchHelper() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if(recyclerView.getAdapter().getItemViewType(viewHolder.getAdapterPosition())
                        == Adapter.TYPE_DEPOSIT) {
                    return 0;
                }

                return super.getMovementFlags(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                ViewHolder holder = (ViewHolder)viewHolder;
                holder.hideInput();

                final int pos = viewHolder.getAdapterPosition();
                final Product product = cart.getProduct(pos);
                final String scannedCode = cart.getScannedCode(pos);
                final int quantity = cart.getQuantity(pos);
                final boolean isZeroAmountProduct = cart.isZeroAmountProduct(pos);

                cart.removeAll(pos);
                Telemetry.event(Telemetry.Event.DeletedFromCart, product);
                recyclerView.getAdapter().notifyItemRemoved(pos);
                update();

                snackbar = UIUtils.snackbar(coordinatorLayout,
                        R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG);
                snackbar.setAction(R.string.Snabble_undo, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ScannableCode parsedCode = ScannableCode.parse(SnabbleUI.getProject(), scannedCode);
                        cart.insert(product, pos, quantity, parsedCode, isZeroAmountProduct);
                        recyclerView.getAdapter().notifyDataSetChanged();
                        Telemetry.event(Telemetry.Event.UndoDeleteFromCart, product);
                    }
                });
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onStateChanged(Checkout.State state) {
        if (state == Checkout.State.HANDSHAKING) {
            progressDialog.showAfterDelay(500);
        } else if (state == Checkout.State.REQUEST_PAYMENT_METHOD || state == Checkout.State.WAIT_FOR_APPROVAL) {
            SnabbleUICallback callback = SnabbleUI.getUiCallback();
            if (callback != null) {
                callback.showCheckout();
            }
            progressDialog.dismiss();
        } else if (state == Checkout.State.CONNECTION_ERROR) {
            UIUtils.snackbar(coordinatorLayout, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                    .show();
            progressDialog.dismiss();
        } else if (state != Checkout.State.VERIFYING_PAYMENT_METHOD) {
            progressDialog.dismiss();
        }
    }

    private void updateEmptyState() {
        if (cart.size() > 0) {
            emptyState.setVisibility(View.GONE);
            pay.setVisibility(checkout.isAvailable() ? View.VISIBLE : View.GONE);
        } else {
            emptyState.setVisibility(View.VISIBLE);
            pay.setVisibility(View.GONE);
        }
    }

    private void updatePayText() {
        if (cart != null) {
            int quantity = cart.getTotalQuantity();
            String price = priceFormatter.format(cart.getTotalPrice());

            pay.setText(getResources().getQuantityString(R.plurals.Snabble_Shoppingcart_buyProducts,
                    quantity, quantity, price));
        }
    }

    private void onCartUpdated() {
        update();

        recyclerViewAdapter.notifyDataSetChanged();
    }

    private void update() {
        updatePayText();
        updateEmptyState();
        scanForImages();

        Adapter adapter = (Adapter) recyclerView.getAdapter();
        adapter.updateDeposit();

        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    private void scanForImages() {
        hasAnyImages = false;

        for(int i=0; i<cart.size(); i++) {
            Product product = cart.getProduct(i);
            String url = product.getImageUrl();
            if(url != null && url.length() > 0) {
                hasAnyImages = true;
                break;
            }
        }
    }

    public void registerListeners() {
        cart.addListener(shoppingCartListener);
        checkout.addOnCheckoutStateChangedListener(ShoppingCartView.this);
    }

    public void unregisterListeners() {
        cart.removeListener(shoppingCartListener);
        checkout.removeOnCheckoutStateChangedListener(ShoppingCartView.this);

        progressDialog.dismiss();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        registerListeners();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        unregisterListeners();
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        registerListeners();
                        onCartUpdated();
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        unregisterListeners();
                    }
                }
            };

    private class ViewHolder extends RecyclerView.ViewHolder {
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

        TextWatcher textWatcher;

        public ViewHolder(View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.image);
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
        }

        @SuppressLint("SetTextI18n")
        public void bindTo(final int position) {
            final Product product = cart.getProduct(position);
            final int quantity = cart.getQuantity(position);
            final Integer embeddedPrice = cart.getEmbeddedPrice(position);
            final Integer embeddedAmount = cart.getEmbeddedUnits(position);
            final Integer embeddedWeight = cart.getEmbeddedWeight(position);
            RoundingMode roundingMode = SnabbleUI.getProject().getRoundingMode();

            if (product != null) {
                Product.Type type = product.getType();

                name.setText(product.getName());

                String price = priceFormatter.format(product);

                if(embeddedPrice != null){
                    priceTextView.setText(" " + priceFormatter.format(embeddedPrice));
                } else if(embeddedAmount != null){
                    priceTextView.setText(String.format(" * %s = %s",
                            priceFormatter.format(product.getPrice()),
                            priceFormatter.format(product.getPrice() * embeddedAmount)));
                } else if(embeddedWeight != null){
                    String priceSum = priceFormatter.format(product.getPriceForQuantity(embeddedWeight, roundingMode));
                    priceTextView.setText(String.format(" * %s = %s", price, priceSum));
                } else if (quantity == 1) {
                    priceTextView.setText(" " + price);
                } else {
                    String priceSum = priceFormatter.format(product.getPriceForQuantity(quantity, roundingMode));
                    priceTextView.setText(String.format(" * %s = %s", price, priceSum));
                }

                if(embeddedWeight != null) {
                    quantityTextView.setText(String.format("%s g", String.valueOf(embeddedWeight)));
                } else if (embeddedAmount != null) {
                    quantityTextView.setText(String.valueOf(embeddedAmount));
                } else if (type == Product.Type.UserWeighed) {
                    quantityTextView.setText(String.format("%s g", String.valueOf(quantity)));
                } else {
                    quantityTextView.setText(String.valueOf(quantity));
                }

                if (product.getSubtitle() == null || product.getSubtitle().equals("")) {
                    subtitle.setVisibility(View.GONE);
                } else {
                    subtitle.setText(product.getSubtitle());
                }

                switch (type) {
                    case Article:
                        if (embeddedAmount != null) {
                            if (cart.isZeroAmountProduct(position)) {
                                controlsDefault.setVisibility(View.VISIBLE);
                            } else {
                                controlsDefault.setVisibility(View.GONE);
                            }

                            controlsUserWeighed.setVisibility(View.GONE);
                        } else {
                            if(embeddedPrice != null) {
                                controlsDefault.setVisibility(View.GONE);
                                controlsUserWeighed.setVisibility(View.GONE);
                            } else {
                                controlsDefault.setVisibility(View.VISIBLE);
                                controlsUserWeighed.setVisibility(View.INVISIBLE);
                            }
                        }
                        break;
                    case PreWeighed:
                        if (cart.isZeroAmountProduct(position)) {
                            controlsDefault.setVisibility(View.VISIBLE);
                        } else {
                            controlsDefault.setVisibility(View.GONE);
                        }

                        controlsUserWeighed.setVisibility(View.GONE);
                        break;
                    case UserWeighed:
                        controlsDefault.setVisibility(View.INVISIBLE);
                        controlsUserWeighed.setVisibility(View.VISIBLE);
                        break;
                }

                plus.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int p = getAdapterPosition();

                        if(cart.isZeroAmountProduct(p)){
                            cart.setScannedCode(p,
                                    EAN13.generateNewCodeWithEmbeddedData(SnabbleUI.getProject(),
                                    cart.getScannedCode(p), embeddedAmount + 1));
                        } else {
                            cart.setQuantity(p, quantity + 1);
                        }

                        recyclerViewAdapter.notifyItemChanged(getAdapterPosition());
                        update();
                    }
                });

                minus.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int p = getAdapterPosition();

                        String str = getResources().getString(
                                R.string.Snabble_Shoppingcart_removeItem,
                                product.getName());
                        boolean isZeroAmountProduct = cart.isZeroAmountProduct(p);
                        int q = isZeroAmountProduct ? embeddedAmount - 1 : quantity - 1;
                        if (q <= 0) {
                            new AlertDialog.Builder(getContext())
                                    .setMessage(str)
                                    .setPositiveButton(R.string.Snabble_Yes,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    cart.removeAll(getAdapterPosition());
                                                    recyclerViewAdapter.notifyItemChanged(getAdapterPosition());
                                                    update();
                                                    Telemetry.event(Telemetry.Event.DeletedFromCart, product);
                                                }
                                            })
                                    .setNegativeButton(R.string.Snabble_No, null)
                                    .create()
                                    .show();
                        } else {
                            if(isZeroAmountProduct){
                                cart.setScannedCode(p,
                                        EAN13.generateNewCodeWithEmbeddedData(SnabbleUI.getProject(),
                                                cart.getScannedCode(p), q));
                            } else {
                                cart.setQuantity(p, q);
                            }

                            recyclerViewAdapter.notifyItemChanged(getAdapterPosition());
                            update();
                        }
                    }
                });

                quantityEditApply.setOnClickListener(new OneShotClickListener() {
                    @Override
                    public void click() {
                        int pos = getAdapterPosition();
                        cart.setQuantity(pos, getQuantityEditValue());
                        recyclerViewAdapter.notifyItemChanged(pos);
                        update();

                        hideInput();
                    }
                });

                quantityEdit.setText(String.valueOf(quantity));
                itemView.setFocusable(true);
                itemView.setFocusableInTouchMode(true);

                if (position == 0) {
                    itemView.requestFocus();
                }

                quantityEdit.removeTextChangedListener(textWatcher);
                textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        updateQuantityEditApplyVisibility(quantity);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                };

                updateQuantityEditApplyVisibility(quantity);

                quantityEdit.addTextChangedListener(textWatcher);
                quantityEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE
                                || (event.getAction() == KeyEvent.ACTION_DOWN
                                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                            quantityEditApply.callOnClick();
                            return true;
                        }

                        return false;
                    }
                });

                String imageUrl = product.getImageUrl();
                if (imageUrl != null && !imageUrl.equals("")) {
                    image.setVisibility(View.VISIBLE);
                    Picasso.with(getContext()).load(imageUrl).into(image);
                } else {
                    image.setVisibility(hasAnyImages ? View.INVISIBLE : View.GONE);
                    image.setImageBitmap(null);
                }
            } else {
                itemView.setVisibility(View.GONE);
            }
        }

        private void hideInput() {
            InputMethodManager imm = (InputMethodManager) getContext()
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
            } else {
                quantityEditApply.setVisibility(View.GONE);
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

    private class DepositViewHolder extends RecyclerView.ViewHolder {
        TextView deposit;
        AppCompatImageView image;

        public DepositViewHolder(View itemView) {
            super(itemView);

            deposit = itemView.findViewById(R.id.deposit);
            image = itemView.findViewById(R.id.image);
        }

        public void update() {
            PriceFormatter priceFormatter = new PriceFormatter(SnabbleUI.getProject());
            String depositText = priceFormatter.format(cart.getTotalDepositPrice());
            deposit.setText(depositText);
        }
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public static final int TYPE_ITEM = 0;
        public static final int TYPE_DEPOSIT = 1;

        @Override
        public int getItemViewType(int position) {
            if(cart.getTotalDepositPrice() > 0 && position == getItemCount() - 1){
                return TYPE_DEPOSIT;
            }

            return TYPE_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType == TYPE_DEPOSIT) {
                View v = View.inflate(getContext(), R.layout.item_shoppingcart_deposit, null);
                return new DepositViewHolder(v);
            } else {
                View v = View.inflate(getContext(), R.layout.item_shoppingcart_product, null);
                return new ViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            if(getItemViewType(position) == TYPE_DEPOSIT){
                DepositViewHolder viewHolder = (DepositViewHolder)holder;
                viewHolder.update();
            } else {
                ViewHolder viewHolder = (ViewHolder)holder;
                viewHolder.bindTo(position);
            }
        }

        public void updateDeposit() {
            for(int i=0; i<getItemCount(); i++) {
                if(getItemViewType(i) == TYPE_DEPOSIT) {
                    notifyItemChanged(i);
                }
            }
        }

        @Override
        public int getItemCount() {
            if(cart.getTotalDepositPrice() > 0){
                return cart.size() + 1;
            }

            return cart.size();
        }
    }
}

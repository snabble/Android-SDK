package io.snabble.sdk.ui.cart;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.*;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.utils.GsonHolder;

public class PaymentSelectionHelper {
    private static PaymentSelectionHelper instance = new PaymentSelectionHelper();

    public static PaymentSelectionHelper getInstance() {
        return instance;
    }

    public class Entry implements Serializable {
        String text;
        String hint;
        transient int iconResId;
        PaymentCredentials paymentCredentials;
        PaymentMethod paymentMethod;
        boolean isAvailable;
    }

    private Map<PaymentMethod, Integer> icons = new HashMap<>();
    private Map<PaymentMethod, String> names = new HashMap<>();

    private Context context;
    private PaymentCredentialsStore paymentCredentialsStore;
    private List<PaymentCredentials> paymentCredentials;
    private MutableLiveData<Entry> selectedEntry;
    private Project project;
    private ShoppingCart cart;
    private SharedPreferences sharedPreferences;
    private ArrayList<Entry> entries;
    private boolean isOffline;
    private WeakReference<DialogFragment> existingDialogFragment;
    private PaymentCredentials lastAddedPaymentCredentials;

    private ShoppingCart.ShoppingCartListener shoppingCartListener =
            new ShoppingCart.SimpleShoppingCartListener() {
                @Override
                public void onChanged(ShoppingCart list) {
                    update();
                }
            };

    private PaymentSelectionHelper() {
        context = Snabble.getInstance().getApplication();

        icons.put(PaymentMethod.DE_DIRECT_DEBIT, R.drawable.snabble_ic_payment_select_sepa);
        icons.put(PaymentMethod.VISA, R.drawable.snabble_ic_payment_select_visa);
        icons.put(PaymentMethod.MASTERCARD, R.drawable.snabble_ic_payment_select_mastercard);
        icons.put(PaymentMethod.AMEX, R.drawable.snabble_ic_payment_select_amex);
        icons.put(PaymentMethod.PAYDIREKT, R.drawable.snabble_ic_payment_select_paydirekt);
        icons.put(PaymentMethod.TEGUT_EMPLOYEE_CARD, R.drawable.snabble_ic_payment_select_tegut);
        icons.put(PaymentMethod.GATEKEEPER_TERMINAL, R.drawable.snabble_ic_payment_select_sco);
        icons.put(PaymentMethod.QRCODE_POS, R.drawable.snabble_ic_payment_select_pos);
        icons.put(PaymentMethod.QRCODE_OFFLINE, R.drawable.snabble_ic_payment_select_pos);

        names.put(PaymentMethod.DE_DIRECT_DEBIT, "SEPA-Lastschrift");
        names.put(PaymentMethod.VISA, "VISA");
        names.put(PaymentMethod.MASTERCARD, "Mastercard");
        names.put(PaymentMethod.AMEX, "American Express");
        names.put(PaymentMethod.PAYDIREKT, "Paydirekt");
        names.put(PaymentMethod.TEGUT_EMPLOYEE_CARD, "Tegut... Mitarbeiterkarte");
        names.put(PaymentMethod.GATEKEEPER_TERMINAL, context.getString(R.string.Snabble_Payment_payAtSCO));
        names.put(PaymentMethod.QRCODE_POS, context.getString(R.string.Snabble_Payment_payAtCashDesk));
        names.put(PaymentMethod.QRCODE_OFFLINE, context.getString(R.string.Snabble_Payment_payAtCashDesk));

        selectedEntry = new MutableLiveData<>();

        paymentCredentialsStore = Snabble.getInstance().getPaymentCredentialsStore();
        paymentCredentialsStore.addCallback(this::update);

        paymentCredentialsStore.addOnPaymentCredentialsAddedListener(paymentCredentials -> {
            lastAddedPaymentCredentials = paymentCredentials;
            update();
        });

        sharedPreferences = Snabble.getInstance().getApplication()
                .getSharedPreferences("snabble_cart", Context.MODE_PRIVATE);

        update();

        SnabbleUI.getProjectAsLiveData().observeForever(project -> {
            PaymentSelectionHelper.this.project = project;

            if (cart != null) {
                cart.removeListener(shoppingCartListener);
            }

            cart = project.getShoppingCart();
            cart.addListener(shoppingCartListener);

            update();
        });
    }

    private void update() {
        paymentCredentials = paymentCredentialsStore.getAllWithoutKeyStoreValidation();
        updateEntries();

        if (entries.size() > 0 && cart.size() > 0) {
            if (lastAddedPaymentCredentials != null) {
                for (Entry e : entries) {
                    if (e.paymentCredentials != null) {
                        if (e.paymentCredentials.getId().equals(lastAddedPaymentCredentials.getId())) {
                            select(e);
                            lastAddedPaymentCredentials = null;
                            break;
                        }
                    }
                }
            }

            // preserve last payment selection, if its still available
            String last = sharedPreferences.getString("lastPaymentSelection", null);
            Entry lastEntry = null;
            if (last != null) {
                lastEntry = GsonHolder.get().fromJson(last, Entry.class);
                Integer iconResId = icons.get(lastEntry.paymentMethod);
                if (iconResId != null) {
                    lastEntry.iconResId = iconResId;
                }
            }

            if (lastEntry != null) {
                for (Entry e : entries) {
                    if (e.paymentCredentials != null && lastEntry.paymentCredentials != null) {
                        if (e.paymentCredentials.getId().equals(lastEntry.paymentCredentials.getId())) {
                            if (e.isAvailable) {
                                selectedEntry.postValue(e);
                                return;
                            }
                        }
                    } else if (lastEntry.paymentMethod == e.paymentMethod) {
                        if (e.isAvailable) {
                            selectedEntry.postValue(e);
                            return;
                        }
                    }
                }
            }

            // payment credentials or payment method were not available, use defaults
            for (Entry e : entries) {
                if (e.isAvailable) {
                    selectedEntry.postValue(e);
                    break;
                }
            }
        } else {
            selectedEntry.postValue(null);
        }
    }

    private void save(Entry e) {
        String json = GsonHolder.get().toJson(e);

        sharedPreferences.edit()
                .putString("lastPaymentSelection", json)
                .apply();
    }

    private void updateEntries() {
        ArrayList<Entry> entries = new ArrayList<>();

        if (cart == null) {
            this.entries = entries;
            this.isOffline = false;
            return;
        }

        List<PaymentMethod> projectPaymentMethods = Arrays.asList(project.getAvailablePaymentMethods());
        CheckoutApi.PaymentMethodInfo[] availablePaymentMethods = cart.getAvailablePaymentMethods();
        if (availablePaymentMethods == null) {
            for (PaymentMethod paymentMethod : projectPaymentMethods) {
                if (paymentMethod.isOfflineMethod()) {
                    final Entry e = new Entry();
                    e.text = names.get(paymentMethod);
                    e.paymentMethod = paymentMethod;
                    e.isAvailable = true;

                    Integer iconResId = icons.get(paymentMethod);
                    if (iconResId != null) {
                        e.iconResId = iconResId;
                        entries.add(e);
                    }
                }
            }

            this.entries = entries;
            this.isOffline = !cart.isVerifiedOnline();
            return;
        }

        List<PaymentMethod> availablePaymentMethodsList = new ArrayList<>();
        for (final CheckoutApi.PaymentMethodInfo paymentMethodInfo : availablePaymentMethods) {
            availablePaymentMethodsList.add(PaymentMethod.fromString(paymentMethodInfo.id));
        }

        for (final PaymentCredentials pc : Snabble.getInstance().getPaymentCredentialsStore().getAllWithoutKeyStoreValidation()) {
            final Entry e = new Entry();

            if (!pc.isAvailableInCurrentApp()) {
                continue;
            }

            e.paymentMethod = pc.getPaymentMethod();
            if (e.paymentMethod == null || !pc.isAvailableInCurrentApp()) {
                continue;
            }

            e.text = names.get(e.paymentMethod);
            e.paymentCredentials = pc;

            if (availablePaymentMethodsList.contains(e.paymentMethod)) {
                e.isAvailable = true;
                e.hint = pc.getObfuscatedId();
            } else if (projectPaymentMethods.contains(e.paymentMethod)){
                e.hint = context.getString(R.string.Snabble_Shoppingcart_notForThisPurchase);
                e.isAvailable = false;
            } else {
                e.hint = context.getString(R.string.Snabble_Shoppingcart_notForVendor);
                e.isAvailable = false;
            }

            Integer iconResId = icons.get(e.paymentMethod);
            if (iconResId != null) {
                e.iconResId = iconResId;
                entries.add(e);
            }
        }

        for (PaymentMethod pm : projectPaymentMethods) {
            if (pm.isOfflineMethod() || !pm.isRequiringCredentials()) {
                final Entry e = new Entry();
                e.text = names.get(pm);
                e.paymentMethod = pm;
                e.isAvailable = true;

                Integer iconResId = icons.get(pm);
                if (iconResId != null) {
                    e.iconResId = iconResId;
                    entries.add(e);
                }
            }
        }

        this.entries = entries;
        this.isOffline = false;
    }

    public void select(PaymentSelectionHelper.Entry entry) {
        selectedEntry.postValue(entry);
        save(entry);
    }

    public LiveData<Entry> getSelectedEntry() {
        return selectedEntry;
    }

    public void showDialog(FragmentActivity fragmentActivity) {
        if (existingDialogFragment != null) {
            DialogFragment f = existingDialogFragment.get();
            if (f != null) {
                f.dismissAllowingStateLoss();
                existingDialogFragment.clear();
                existingDialogFragment = null;
            }
        }

        Bundle args = new Bundle();
        args.putSerializable(PaymentSelectionDialogFragment.ARG_ENTRIES, entries);
        args.putBoolean(PaymentSelectionDialogFragment.ARG_SHOW_OFFLINE_HINT, isOffline);

        Entry se = selectedEntry.getValue();
        if (se != null) {
            args.putSerializable("selectedEntry", se);
        }

        DialogFragment dialogFragment = new PaymentSelectionDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.show(fragmentActivity.getSupportFragmentManager(), null);
        existingDialogFragment = new WeakReference<>(dialogFragment);
    }
}

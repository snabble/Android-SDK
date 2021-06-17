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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.snabble.sdk.*;
import io.snabble.sdk.googlepay.GooglePayHelper;
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
        public PaymentCredentials paymentCredentials;
        public PaymentMethod paymentMethod;
        boolean isAvailable;
        boolean isAdded = true;
    }

    private Map<PaymentMethod, Integer> icons = new HashMap<>();
    private Map<PaymentMethod, String> names = new HashMap<>();
    private List<PaymentMethod> paymentMethodsSortPriority = new ArrayList<>();

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
    private boolean googlePayIsReady = false;

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
        icons.put(PaymentMethod.CUSTOMERCARD_POS, R.drawable.snabble_ic_payment_select_pos);
        icons.put(PaymentMethod.GATEKEEPER_TERMINAL, R.drawable.snabble_ic_payment_select_sco);
        icons.put(PaymentMethod.QRCODE_POS, R.drawable.snabble_ic_payment_select_pos);
        icons.put(PaymentMethod.QRCODE_OFFLINE, R.drawable.snabble_ic_payment_select_pos);
        icons.put(PaymentMethod.POST_FINANCE_CARD, R.drawable.snabble_ic_payment_select_postfinance);
        icons.put(PaymentMethod.TWINT, R.drawable.snabble_ic_payment_select_twint);
        icons.put(PaymentMethod.GOOGLE_PAY, R.drawable.snabble_ic_payment_select_gpay);

        names.put(PaymentMethod.DE_DIRECT_DEBIT, "SEPA-Lastschrift");
        names.put(PaymentMethod.VISA, "VISA");
        names.put(PaymentMethod.MASTERCARD, "Mastercard");
        names.put(PaymentMethod.AMEX, "American Express");
        names.put(PaymentMethod.PAYDIREKT, "Paydirekt");
        names.put(PaymentMethod.TEGUT_EMPLOYEE_CARD, "Tegut... Mitarbeiterkarte");
        names.put(PaymentMethod.GATEKEEPER_TERMINAL, context.getString(R.string.Snabble_Payment_payAtSCO));
        names.put(PaymentMethod.QRCODE_POS, context.getString(R.string.Snabble_Payment_payAtCashDesk));
        names.put(PaymentMethod.CUSTOMERCARD_POS, context.getString(R.string.Snabble_Payment_payAtCashDesk));
        names.put(PaymentMethod.QRCODE_OFFLINE, context.getString(R.string.Snabble_Payment_payAtCashDesk));
        names.put(PaymentMethod.POST_FINANCE_CARD, "PostFinance Card");
        names.put(PaymentMethod.TWINT, "Twint");
        names.put(PaymentMethod.GOOGLE_PAY, "Google Pay");

        paymentMethodsSortPriority.add(PaymentMethod.GOOGLE_PAY);
        paymentMethodsSortPriority.add(PaymentMethod.DE_DIRECT_DEBIT);
        paymentMethodsSortPriority.add(PaymentMethod.VISA);
        paymentMethodsSortPriority.add(PaymentMethod.MASTERCARD);
        paymentMethodsSortPriority.add(PaymentMethod.AMEX);
        paymentMethodsSortPriority.add(PaymentMethod.TWINT);
        paymentMethodsSortPriority.add(PaymentMethod.POST_FINANCE_CARD);
        paymentMethodsSortPriority.add(PaymentMethod.PAYDIREKT);
        paymentMethodsSortPriority.add(PaymentMethod.GATEKEEPER_TERMINAL);
        paymentMethodsSortPriority.add(PaymentMethod.TEGUT_EMPLOYEE_CARD);
        paymentMethodsSortPriority.add(PaymentMethod.CUSTOMERCARD_POS);
        paymentMethodsSortPriority.add(PaymentMethod.QRCODE_POS);
        paymentMethodsSortPriority.add(PaymentMethod.QRCODE_OFFLINE);

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
            if (project != null) {
                PaymentSelectionHelper.this.project = project;

                if (cart != null) {
                    cart.removeListener(shoppingCartListener);
                }

                cart = project.getShoppingCart();
                cart.addListener(shoppingCartListener);

                update();
            }
        });
    }

    private void update() {
        updateGooglePayIsReadyToPay();

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
                                setSelectedEntry(e);
                                return;
                            }
                        }
                    } else if (lastEntry.paymentMethod == e.paymentMethod) {
                        if (e.isAvailable) {
                            setSelectedEntry(e);
                            return;
                        }
                    }
                }
            }

            // payment credentials or payment method were not available, use defaults
            Entry preferredDefaultEntry = null;
            for (Entry e : entries) {
                if (e.isAvailable) {
                    preferredDefaultEntry = e;
                    break;
                }
            }

            // google pay always wins if available and the user did not select anything
            for (Entry e : entries) {
                if (e.paymentMethod == PaymentMethod.GOOGLE_PAY) {
                    preferredDefaultEntry = e;
                    break;
                }
            }

            if (preferredDefaultEntry != null) {
                setSelectedEntry(preferredDefaultEntry);
            }
        } else {
            setSelectedEntry(null);
        }
    }

    private void updateGooglePayIsReadyToPay() {
        if (cart != null) {
            CheckoutApi.PaymentMethodInfo[] infos = cart.getAvailablePaymentMethods();
            if (infos != null) {
                GooglePayHelper googlePayHelper = project.getGooglePayHelper();
                if (googlePayHelper != null) {
                    for (CheckoutApi.PaymentMethodInfo info : cart.getAvailablePaymentMethods()) {
                        if (info.id.equals(PaymentMethod.GOOGLE_PAY.id())) {
                            googlePayHelper.setUseTestEnvironment(info.isTesting);
                            break;
                        }
                    }

                    project.getGooglePayHelper().isReadyToPay(isReadyToPay -> {
                        if (googlePayIsReady != isReadyToPay) {
                            googlePayIsReady = isReadyToPay;
                            update();
                        }
                    });
                } else {
                    googlePayIsReady = false;
                }
            } else {
                googlePayIsReady = false;
            }
        } else {
            googlePayIsReady = false;
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
            PaymentMethod paymentMethod = PaymentMethod.fromString(paymentMethodInfo.id);
            if (paymentMethod == PaymentMethod.GOOGLE_PAY && googlePayIsReady) {
                availablePaymentMethodsList.add(paymentMethod);
            } else if (paymentMethod != PaymentMethod.GOOGLE_PAY) {
                availablePaymentMethodsList.add(paymentMethod);
            }
        }

        Set<PaymentMethod> addedCredentialPaymentMethods = new HashSet<PaymentMethod>();

        for (final PaymentCredentials pc : Snabble.getInstance().getPaymentCredentialsStore().getAllWithoutKeyStoreValidation()) {
            final Entry e = new Entry();

            if (!pc.isAvailableInCurrentApp()) {
                continue;
            }

            if (pc.getProjectId() != null && !pc.getProjectId().equals(project.getId())) {
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
                addedCredentialPaymentMethods.add(e.paymentMethod);
            }
        }

        for (PaymentMethod pm : projectPaymentMethods) {
            if (pm == PaymentMethod.GOOGLE_PAY && !googlePayIsReady) {
                continue;
            }

            if (pm.isRequiringCredentials() && addedCredentialPaymentMethods.contains(pm)) {
                continue;
            }

            final Entry e = new Entry();

            e.text = names.get(pm);
            e.paymentMethod = pm;
            e.isAvailable = true;
            e.isAdded = !pm.isRequiringCredentials();

            if (pm.isRequiringCredentials()) {
                e.hint = context.getString(R.string.Snabble_Shoppingcart_noPaymentData);
            }

            Integer iconResId = icons.get(pm);
            if (iconResId != null) {
                e.iconResId = iconResId;
                entries.add(e);
            }
        }

        Collections.sort(entries, (o1, o2) -> {
            int p1 = paymentMethodsSortPriority.indexOf(o1.paymentMethod);
            int p2 = paymentMethodsSortPriority.indexOf(o2.paymentMethod);

            return Integer.compare(p1, p2);
        });

        this.entries = entries;
        this.isOffline = false;
    }


    public void select(PaymentSelectionHelper.Entry entry) {
        setSelectedEntry(entry);
        save(entry);
    }

    private void setSelectedEntry(Entry entry) {
        Entry currentEntry = selectedEntry.getValue();
        if (currentEntry != null && entry != null && currentEntry.paymentMethod != entry.paymentMethod) {
            cart.generateNewUUID();
        }

        selectedEntry.postValue(entry);
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

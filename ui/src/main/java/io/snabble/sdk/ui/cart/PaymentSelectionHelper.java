package io.snabble.sdk.ui.cart;

import android.app.Application;
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
    private static final PaymentSelectionHelper instance = new PaymentSelectionHelper();

    public static PaymentSelectionHelper getInstance() {
        return instance;
    }

    public static class Entry implements Serializable {
        String text;
        String hint;
        transient int iconResId;
        public PaymentCredentials paymentCredentials;
        public PaymentMethod paymentMethod;
        boolean isAvailable;
        boolean isAdded = true;
    }

    private final Map<PaymentMethod, Integer> icons = new HashMap<>();
    private final Map<PaymentMethod, String> names = new HashMap<>();
    private final List<PaymentMethod> paymentMethodsSortPriority = new ArrayList<>();

    private Application application;
    private PaymentCredentialsStore paymentCredentialsStore;
    private List<PaymentCredentials> paymentCredentials;
    private MutableLiveData<Entry> selectedEntry;
    private Project project;
    private ShoppingCart cart;
    private final SharedPreferences sharedPreferences;
    private ArrayList<Entry> entries;
    private boolean isOffline;
    private WeakReference<DialogFragment> existingDialogFragment;
    private PaymentCredentials lastAddedPaymentCredentials;
    private boolean googlePayIsReady = false;

    private final ShoppingCart.ShoppingCartListener shoppingCartListener =
            new ShoppingCart.SimpleShoppingCartListener() {
                @Override
                public void onChanged(ShoppingCart list) {
                    update();
                }
            };

    private PaymentSelectionHelper() {
        application = Snabble.getInstance().getApplication();

        icons.put(PaymentMethod.DE_DIRECT_DEBIT, R.drawable.snabble_ic_payment_select_sepa);
        icons.put(PaymentMethod.VISA, R.drawable.snabble_ic_payment_select_visa);
        icons.put(PaymentMethod.MASTERCARD, R.drawable.snabble_ic_payment_select_mastercard);
        icons.put(PaymentMethod.AMEX, R.drawable.snabble_ic_payment_select_amex);
        icons.put(PaymentMethod.PAYDIREKT, R.drawable.snabble_ic_payment_select_paydirekt);
        icons.put(PaymentMethod.TEGUT_EMPLOYEE_CARD, R.drawable.snabble_ic_payment_select_tegut);
        icons.put(PaymentMethod.LEINWEBER_CUSTOMER_ID, R.drawable.snabble_ic_payment_select_leinweber);
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
        names.put(PaymentMethod.LEINWEBER_CUSTOMER_ID, "Leinweber Rechnungskauf");
        names.put(PaymentMethod.GATEKEEPER_TERMINAL, application.getString(R.string.Snabble_Payment_payAtSCO));
        names.put(PaymentMethod.QRCODE_POS, application.getString(R.string.Snabble_Payment_payAtCashDesk));
        names.put(PaymentMethod.CUSTOMERCARD_POS, application.getString(R.string.Snabble_Payment_payAtCashDesk));
        names.put(PaymentMethod.QRCODE_OFFLINE, application.getString(R.string.Snabble_Payment_payAtCashDesk));
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
        paymentMethodsSortPriority.add(PaymentMethod.LEINWEBER_CUSTOMER_ID);
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
            String last = sharedPreferences.getString(getLastPaymentSelectionKey(), null);
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
                        if (e.isAvailable && e.isAdded) {
                            setSelectedEntry(e);
                            return;
                        }
                    }
                }
            }

            Entry preferredDefaultEntry = null;
            for (Entry e : entries) {
                if (e.paymentMethod.isRequiringCredentials() && e.isAdded) {
                    preferredDefaultEntry = e;
                    break;
                }
            }

            if (entries.size() == 1 && cart.getTotalPrice() >= 0) {
                preferredDefaultEntry = entries.get(0);
            }

            // google pay always wins if available and the user did not select anything
            for (Entry e : entries) {
                if (e.paymentMethod == PaymentMethod.GOOGLE_PAY) {
                    preferredDefaultEntry = e;
                    break;
                }
            }

            setSelectedEntry(preferredDefaultEntry);
        } else {
            setSelectedEntry(null);
        }
    }

    private String getLastPaymentSelectionKey() {
        return "lastPaymentSelection_" + project.getId();
    }

    private void updateGooglePayIsReadyToPay() {
        if (cart != null) {
            CheckoutApi.PaymentMethodInfo[] availablePaymentMethods = cart.getAvailablePaymentMethods();
            if (availablePaymentMethods != null) {
                GooglePayHelper googlePayHelper = project.getGooglePayHelper();
                if (googlePayHelper != null) {
                    for (CheckoutApi.PaymentMethodInfo info : availablePaymentMethods) {
                        if (info.id.equals(PaymentMethod.GOOGLE_PAY.getId())) {
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
                .putString(getLastPaymentSelectionKey(), json)
                .apply();
    }

    private void updateEntries() {
        ArrayList<Entry> entries = new ArrayList<>();

        if (cart == null) {
            this.entries = entries;
            this.isOffline = false;
            return;
        }

        List<PaymentMethod> projectPaymentMethods = project.getAvailablePaymentMethods();
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
            String[] origins = paymentMethodInfo.acceptedOriginTypes;
            if(origins == null) origins = new String[0];
            PaymentMethod paymentMethod = PaymentMethod.fromIdAndOrigin(paymentMethodInfo.id, Arrays.asList(origins));
            if (paymentMethod == PaymentMethod.GOOGLE_PAY && googlePayIsReady) {
                availablePaymentMethodsList.add(paymentMethod);
            } else if (paymentMethod != PaymentMethod.GOOGLE_PAY) {
                availablePaymentMethodsList.add(paymentMethod);
            }
        }

        Set<PaymentMethod> addedCredentialPaymentMethods = new HashSet<>();

        for (final PaymentCredentials pc : Snabble.getInstance().getPaymentCredentialsStore().getAllWithoutKeyStoreValidation()) {
            if (!pc.isAvailableInCurrentApp()) {
                continue;
            }

            if (pc.getProjectId() != null && !pc.getProjectId().equals(project.getId())) {
                continue;
            }

            final Entry e = new Entry();
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
                e.hint = application.getString(R.string.Snabble_Shoppingcart_notForThisPurchase);
                e.isAvailable = false;
            } else {
                continue;
            }

            Integer iconResId = icons.get(e.paymentMethod);
            if (iconResId != null) {
                e.iconResId = iconResId;
                entries.add(e);
                addedCredentialPaymentMethods.add(e.paymentMethod);
            }
        }

        for (PaymentMethod pm : projectPaymentMethods) {
            if (!availablePaymentMethodsList.contains(pm)) {
                continue;
            }

            if (pm == PaymentMethod.GOOGLE_PAY && !googlePayIsReady) {
                continue;
            }

            if (pm.isShowOnlyIfCredentialsArePresent()) {
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
                e.hint = application.getString(R.string.Snabble_Shoppingcart_noPaymentData);
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

    public boolean shouldShowBigSelector() {
        for (Entry e : entries) {
            if ((e.paymentMethod.isRequiringCredentials() && e.isAdded)
              || e.paymentMethod == PaymentMethod.GOOGLE_PAY) {
                return false;
            }
        }

        if (entries.size() == 1 && entries.get(0).paymentMethod.isOfflineMethod()) {
            return false;
        }

        if (cart.getTotalPrice() < 0) {
            return false;
        }

        if (selectedEntry.getValue() != null) {
            return false;
        }

        return shouldShowPayButton();
    }

    public boolean shouldShowPayButton() {
        boolean onlinePaymentAvailable = cart.getAvailablePaymentMethods() != null && cart.getAvailablePaymentMethods().length > 0;
        return cart.getTotalPrice() >= 0 && (onlinePaymentAvailable || selectedEntry.getValue() != null);
    }

    public boolean shouldShowGooglePayButton() {
        Entry entry = selectedEntry.getValue();
        return entry != null && entry.paymentMethod == PaymentMethod.GOOGLE_PAY && cart.getTotalPrice() > 0;
    }

    public boolean shouldShowSmallSelector() {
        return cart.size() > 0 && (selectedEntry.getValue() != null || !shouldShowBigSelector());
    }

    private void setSelectedEntry(Entry entry) {
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
            args.putSerializable(PaymentSelectionDialogFragment.ARG_SELECTED_ENTRY, se);
        }

        DialogFragment dialogFragment = new PaymentSelectionDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.show(fragmentActivity.getSupportFragmentManager(), null);
        existingDialogFragment = new WeakReference<>(dialogFragment);
    }
}
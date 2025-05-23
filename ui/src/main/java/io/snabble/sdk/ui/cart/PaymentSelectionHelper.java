package io.snabble.sdk.ui.cart;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.shoppingcart.ShoppingCart;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.checkout.PaymentMethodInfo;
import io.snabble.sdk.googlepay.GooglePayHelper;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener;
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.payment.PaymentMethodMetaDataHelper;
import io.snabble.sdk.utils.GsonHolder;

public class PaymentSelectionHelper {
    private static final PaymentSelectionHelper instance = new PaymentSelectionHelper();

    public static PaymentSelectionHelper getInstance() {
        return instance;
    }

    public static class Entry implements Serializable {
        String text;
        String hint;
        public transient int iconResId;
        public PaymentCredentials paymentCredentials;
        public PaymentMethod paymentMethod;
        boolean isAvailable;
        boolean isAdded = true;
    }

    private final Application application;
    private final MutableLiveData<Entry> selectedEntry;
    private Project project;
    private ShoppingCart cart;
    private final SharedPreferences sharedPreferences;
    private ArrayList<Entry> entries;
    private boolean isOffline;
    private WeakReference<DialogFragment> existingDialogFragment;
    private PaymentCredentials lastAddedPaymentCredentials;
    private boolean googlePayIsReady = false;

    @NonNull
    private final PaymentMethodMetaDataHelper metaDataHelper;

    private final ShoppingCartListener shoppingCartListener =
            new SimpleShoppingCartListener() {
                @Override
                public void onChanged(@NonNull ShoppingCart cart) {
                    update();
                }
            };

    private PaymentSelectionHelper() {
        application = Snabble.getInstance().getApplication();

        metaDataHelper = new PaymentMethodMetaDataHelper(application);

        selectedEntry = new MutableLiveData<>();

        PaymentCredentialsStore paymentCredentialsStore = Snabble.getInstance().getPaymentCredentialsStore();
        paymentCredentialsStore.addCallback(this::update);

        paymentCredentialsStore.addOnPaymentCredentialsAddedListener(paymentCredentials -> {
            lastAddedPaymentCredentials = paymentCredentials;
            update();
        });

        sharedPreferences = Snabble.getInstance().getApplication()
                .getSharedPreferences("snabble_cart", Context.MODE_PRIVATE);

        setProject(Snabble.getInstance().getCheckedInProject().getValue());
        Snabble.getInstance().getCheckedInProject().observeForever(this::setProject);
        update();
    }

    private void setProject(Project project) {
        if (project != null) {
            PaymentSelectionHelper.this.project = project;

            updateShoppingCart();
            update();
        }
    }

    public void updateShoppingCart() {
        if (cart != null) {
            cart.removeListener(shoppingCartListener);
        }

        cart = project.getShoppingCart();
        cart.addListener(shoppingCartListener);
        update();
    }

    private void update() {
        updateGooglePayIsReadyToPay();

        updateEntries();

        if (!entries.isEmpty() && !cart.isEmpty()) {
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
                Integer iconResId = metaDataHelper.iconFor(lastEntry.paymentMethod);
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
            List<PaymentMethodInfo> availablePaymentMethods = cart.getAvailablePaymentMethods();
            if (availablePaymentMethods != null) {
                GooglePayHelper googlePayHelper = project.getGooglePayHelper();
                if (googlePayHelper != null) {
                    for (PaymentMethodInfo info : availablePaymentMethods) {
                        if (Objects.equals(info.getId(), PaymentMethod.GOOGLE_PAY.getId())) {
                            googlePayHelper.setUseTestEnvironment(info.isTesting());
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
        List<PaymentMethodInfo> availablePaymentMethods = cart.getAvailablePaymentMethods();
        if (availablePaymentMethods == null) {
            for (PaymentMethod paymentMethod : projectPaymentMethods) {
                if (paymentMethod.isOfflineMethod()) {
                    final Entry e = new Entry();
                    e.text = metaDataHelper.labelFor(paymentMethod);
                    e.paymentMethod = paymentMethod;
                    e.isAvailable = true;

                    Integer iconResId = metaDataHelper.iconFor(paymentMethod);
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
        for (final PaymentMethodInfo paymentMethodInfo : availablePaymentMethods) {
            List<String> origins = paymentMethodInfo.getAcceptedOriginTypes();
            PaymentMethod paymentMethod = PaymentMethod.fromIdAndOrigin(paymentMethodInfo.getId(), origins);
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

            e.text = metaDataHelper.labelFor(e.paymentMethod);
            e.paymentCredentials = pc;

            if (availablePaymentMethodsList.contains(e.paymentMethod)) {
                if (e.paymentMethod == PaymentMethod.EXTERNAL_BILLING) {
                    e.isAvailable = true;
                    e.hint = pc.getObfuscatedId();
                } else {
                    e.isAvailable = true;
                    e.hint = pc.getObfuscatedId();
                }
            } else if (projectPaymentMethods.contains(e.paymentMethod)) {
                e.hint = application.getString(R.string.Snabble_Shoppingcart_notForThisPurchase);
                e.isAvailable = false;
            } else {
                continue;
            }

            Integer iconResId = metaDataHelper.iconFor(e.paymentMethod);
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

            e.text = metaDataHelper.labelFor(pm);
            e.paymentMethod = pm;
            e.isAvailable = true;
            e.isAdded = !pm.isRequiringCredentials();

            if (pm.isRequiringCredentials()) {
                e.hint = application.getString(R.string.Snabble_Shoppingcart_noPaymentData);
            }

            Integer iconResId = metaDataHelper.iconFor(pm);
            if (iconResId != null) {
                e.iconResId = iconResId;
                entries.add(e);
            }
        }

        Collections.sort(entries, (o1, o2) -> {
            int p1 = metaDataHelper.indexOf(o1.paymentMethod);
            int p2 = metaDataHelper.indexOf(o2.paymentMethod);

            return Integer.compare(p1, p2);
        });

        Entry selected = findMatchingEntry(entries, selectedEntry.getValue());
        if (selected != null && !selected.isAvailable) {
            for (Entry entry : entries) {
                if (entry.isAdded && entry.isAvailable) {
                    select(entry);
                    break;
                }
            }
        }

        this.entries = entries;
        this.isOffline = false;
    }

    private Entry findMatchingEntry(List<Entry> entries, Entry entry) {
        if (entry == null) {
            return null;
        }

        for (Entry e : entries) {
            if (e.paymentMethod == entry.paymentMethod &&
                    (e.paymentCredentials == null || (e.paymentCredentials.getId().equals(entry.paymentCredentials.getId())))) {
                return e;
            }
        }

        return null;
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
        final ShoppingCart shoppingCart = cart;
        boolean onlinePaymentAvailable = shoppingCart.getAvailablePaymentMethods() != null && !shoppingCart.getAvailablePaymentMethods().isEmpty();
        return shoppingCart.getTotalPrice() >= 0 && (onlinePaymentAvailable || selectedEntry.getValue() != null);
    }

    public boolean shouldShowGooglePayButton() {
        Entry entry = selectedEntry.getValue();
        return entry != null && entry.paymentMethod == PaymentMethod.GOOGLE_PAY && cart.getTotalPrice() > 0;
    }

    public boolean shouldShowSmallSelector() {
        return !cart.isEmpty() && (selectedEntry.getValue() != null || !shouldShowBigSelector());
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

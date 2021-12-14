package io.snabble.sdk.ui.payment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputLayout;

import io.snabble.sdk.PaymentOriginCandidateHelper;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.IBAN;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.Keyguard;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.KeyguardUtils;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class SEPACardInputView extends FrameLayout {
    public interface OnCloseListener {
        void onClose();
    }

    private Button save;
    private TextView hint;
    private EditText nameInput;
    private EditText ibanCountryCode;
    private EditText ibanInput;
    private TextInputLayout nameTextInputLayout;
    private TextInputLayout ibanCountryCodeTextInputLayout;
    private TextInputLayout ibanTextInputLayout;
    private boolean acceptedKeyguard;
    private boolean isAttachedToWindow;
    private ProgressBar progressIndicator;
    private PaymentCredentials paymentCredentials;

    private PaymentOriginCandidateHelper.PaymentOriginCandidate prefilledPaymentOriginCandidate;
    public OnCloseListener onCloseListener;

    public SEPACardInputView(Context context) {
        super(context);
        inflateView();
    }

    public SEPACardInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public SEPACardInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_cardinput_sepa, this);

        hint = findViewById(R.id.hint);
        nameInput = findViewById(R.id.input_name);
        ibanCountryCode = findViewById(R.id.prefix);
        ibanInput = findViewById(R.id.input_iban);
        progressIndicator = findViewById(R.id.progress_indicator);

        nameTextInputLayout = findViewById(R.id.input_name_layout);
        ibanCountryCodeTextInputLayout = findViewById(R.id.prefix_layout);
        ibanTextInputLayout = findViewById(R.id.input_iban_layout);

        ibanTextInputLayout.setHelperText(" ");
        nameTextInputLayout.setHelperText(" ");

        save = findViewById(R.id.save);
        save.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                saveCard();
            }
        });

        ibanInput.setFilters(new InputFilter[] {
                new InputFilter.AllCaps()
        });

        ibanCountryCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // update text of iban to adjust spacing
                formatIBANInput();
            }
        });

        ibanInput.addTextChangedListener(new TextWatcher() {
            boolean isUpdating;
            private int previousLength;
            private boolean backSpace;

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isUpdating) {
                        return;
                    }

                    backSpace = previousLength > s.length();

                    if (backSpace) {
                        return;
                    }

                    String originalInput = s.toString();

                    if(originalInput.length() >= 2) {
                        char[] possibleCountryData = originalInput.substring(0, 2).toCharArray();
                        if (Character.isLetter(possibleCountryData[0]) && Character.isLetter(possibleCountryData[1])) {
                            ibanCountryCode.setText(originalInput.substring(0, 2));
                            originalInput = originalInput.substring(2);
                        }
                    }

                    String str = originalInput.replace(" ", "");
                    StringBuilder sb = new StringBuilder();

                    for (int i = 0; i < str.length(); i++) {
                        if ((i + 2) % 4 == 0) {
                            sb.append(' ');
                        }

                        sb.append(str.charAt(i));
                    }

                    isUpdating = true;

                    String text = sb.toString();
                    int selection = ibanInput.getSelectionEnd();
                    selection += Math.max(0, text.length() - originalInput.length());

                    // not using setText because that causes the keyboard state to be reset
                    ibanInput.getText().replace(0, ibanInput.getText().length(), text, 0, text.length());
                    ibanInput.setSelection(Math.min(ibanInput.length(), selection));

                    isUpdating = false;
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousLength = s.length();
            }

            public void afterTextChanged(Editable s) {
                ibanTextInputLayout.setErrorEnabled(false);
            }
        });

        nameInput.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void afterTextChanged(Editable s) {
                nameTextInputLayout.setErrorEnabled(false);
            }
        });

        nameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ibanInput.requestFocus();
                return true;
            }

            return false;
        });

        ibanInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCard();
                return true;
            }

            return false;
        });

        formatIBANInput();
    }

    public void setPrefilledPaymentOriginCandidate(PaymentOriginCandidateHelper.PaymentOriginCandidate paymentOriginCandidate) {
        ibanCountryCode.setText(paymentOriginCandidate.origin.substring(0, 2));
        ibanInput.setText(paymentOriginCandidate.origin.substring(2));
        ibanInput.setEnabled(false);
        ibanCountryCode.setEnabled(false);
        hint.setText(R.string.Snabble_SEPA_scoTransferHint);
        prefilledPaymentOriginCandidate = paymentOriginCandidate;
    }

    private void formatIBANInput() {
        ibanInput.setKeyListener(null);
        ibanInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }

    private void saveCard() {
        boolean ok = true;

        final String name = nameInput.getText().toString();
        final String iban = ibanCountryCode.getText().toString() + ibanInput.getText().toString().replace(" ", "");

        if(name.length() > 0) {
            nameTextInputLayout.setErrorEnabled(false);
        } else {
            nameTextInputLayout.setErrorEnabled(true);
            nameTextInputLayout.setError(getResources().getString(R.string.Snabble_Payment_SEPA_InvalidName));
            ok = false;
        }

        if(IBAN.validate(iban)) {
            ibanTextInputLayout.setErrorEnabled(false);
        } else {
            ibanTextInputLayout.setErrorEnabled(true);
            ibanTextInputLayout.setError(getResources().getString(R.string.Snabble_Payment_SEPA_InvalidIBAN));
            ok = false;
        }

        if (ok) {
            if (KeyguardUtils.isDeviceSecure()) {
                Keyguard.unlock(UIUtils.getHostFragmentActivity(getContext()), new Keyguard.Callback() {
                    @Override
                    public void success() {
                        add(name, iban);
                    }

                    @Override
                    public void error() {

                    }
                });
            } else {
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                        .setPositiveButton(R.string.Snabble_OK, null)
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void add(String name, String iban) {
        paymentCredentials = PaymentCredentials.fromSEPA(name, iban);
        if (paymentCredentials == null) {
            Toast.makeText(getContext(), "Could not verify payment credentials", Toast.LENGTH_LONG)
                    .show();
        } else {
            Snabble.getInstance().getPaymentCredentialsStore().add(paymentCredentials);
            Telemetry.event(Telemetry.Event.PaymentMethodAdded, paymentCredentials.getType().name());
        }

        if (isShown() || !isAttachedToWindow) {
            finish();
        } else {
            acceptedKeyguard = true;
        }
    }

    private void finish() {
        if (prefilledPaymentOriginCandidate != null) {
            progressIndicator.setVisibility(View.VISIBLE);

            prefilledPaymentOriginCandidate.promote(paymentCredentials, new PaymentOriginCandidateHelper.PromoteResult() {
                @Override
                public void success() {
                    Dispatch.mainThread(() -> {
                        progressIndicator.setVisibility(View.GONE);
                        close();
                    });
                }

                @Override
                public void error() {
                    Dispatch.mainThread(() -> {
                        progressIndicator.setVisibility(View.GONE);
                        Toast.makeText(getContext(), R.string.Snabble_networkError, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            close();
        }
    }

    private void close() {
        hideSoftKeyboard(ibanInput);
        hideSoftKeyboard(nameInput);
        SnabbleUI.executeAction(getContext(), SnabbleUI.Action.GO_BACK);

        if (onCloseListener != null) {
            onCloseListener.onClose();
        }
    }

    public OnCloseListener getOnCloseListener() {
        return onCloseListener;
    }

    public void setOnCloseListener(OnCloseListener onCloseListener) {
        this.onCloseListener = onCloseListener;
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        isAttachedToWindow = true;

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        isAttachedToWindow = false;

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        if (acceptedKeyguard) {
                            finish();
                            acceptedKeyguard = false;
                        }
                    }
                }
            };
}
package io.snabble.sdk.ui.payment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.IBAN;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.KeyguardHandler;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class SEPACardInputView extends FrameLayout {
    private Button save;
    private EditText nameInput;
    private EditText ibanCountryCode;
    private EditText ibanInput;
    private TextInputLayout nameTextInputLayout;
    private TextInputLayout ibanCountryCodeTextInputLayout;
    private TextInputLayout ibanTextInputLayout;
    private boolean acceptedKeyguard;
    private boolean isAttachedToWindow;

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

        nameInput = findViewById(R.id.input_name);
        ibanCountryCode = findViewById(R.id.prefix);
        ibanInput = findViewById(R.id.input_iban);

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

        nameInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    ibanInput.requestFocus();
                    return true;
                }

                return false;
            }
        });

        ibanInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveCard();
                    return true;
                }

                return false;
            }
        });

        formatIBANInput();
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
            if (Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()) {
                SnabbleUI.getUiCallback().requestKeyguard(new KeyguardHandler() {
                    @Override
                    public void onKeyguardResult(int resultCode) {
                        if (resultCode == Activity.RESULT_OK) {
                            add(name, iban);
                        }
                    }
                });
            } else {
                add(name, iban);
            }
        }
    }

    private void add(String name, String iban) {
        final PaymentCredentials pc = PaymentCredentials.fromSEPA(name, iban);
        if (pc == null) {
            Toast.makeText(getContext(), "Could not verify payment credentials", Toast.LENGTH_LONG)
                    .show();
        } else {
            Snabble.getInstance().getPaymentCredentialsStore().add(pc);
        }

        if (isShown() || !isAttachedToWindow) {
            finish();
        } else {
            acceptedKeyguard = true;
        }
    }

    private void finish() {
        SnabbleUICallback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            hideSoftKeyboard(ibanInput);
            hideSoftKeyboard(nameInput);
            callback.goBack();
        }
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
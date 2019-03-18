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
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.IBAN;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.KeyguardHandler;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class SEPACardInputView extends FrameLayout {
    private Button save;
    private EditText nameInput;
    private EditText ibanCountryCode;
    private EditText ibanInput;
    private View nameError;
    private View ibanError;
    private boolean acceptedKeyguard;

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
        inflate(getContext(), R.layout.view_cardinput_sepa, this);

        nameInput = findViewById(R.id.input_name);
        nameError = findViewById(R.id.input_invalid_name);

        ibanCountryCode = findViewById(R.id.prefix);
        ibanInput = findViewById(R.id.input_iban);
        ibanError = findViewById(R.id.input_invalid_iban);

        save = findViewById(R.id.save);
        save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
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

                ibanInput.setBackgroundResource(R.drawable.ic_round_edittext);
                ibanCountryCode.setBackgroundResource(R.drawable.ic_round_edittext);
            }
        });

        ibanCountryCode.setEnabled(false);

        ibanInput.addTextChangedListener(new TextWatcher() {
            boolean isUpdating;

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isUpdating) {
                        return;
                    }

                    String originalInput = s.toString();
                    String str = originalInput.replace(" ", "");
                    StringBuilder sb = new StringBuilder();

                    for (int i = 0; i < str.length(); i++) {
                        if (isGermanIBAN()) {
                            if ((i + 2) % 4 == 0) {
                                sb.append(' ');
                            }
                        } else {
                            if (i == 2) {
                                sb.append(' ');
                            }
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

            }

            public void afterTextChanged(Editable s) {
                ibanError.setVisibility(View.INVISIBLE);
                ibanInput.setBackgroundResource(R.drawable.ic_round_edittext);
                ibanCountryCode.setBackgroundResource(R.drawable.ic_round_edittext);
            }
        });

        nameInput.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void afterTextChanged(Editable s) {
                nameError.setVisibility(View.INVISIBLE);
                nameInput.setBackgroundResource(R.drawable.ic_round_edittext);
            }
        });

        formatIBANInput();
    }

    private void formatIBANInput() {
        String str = ibanInput.getText().toString();
        if(isGermanIBAN()) {
            ibanInput.setText(str.replace("", " "));
            ibanInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            ibanInput.setKeyListener(DigitsKeyListener.getInstance("0123456789 "));
        } else {
            ibanInput.setText(str.replace(" ", ""));
            ibanInput.setKeyListener(null);
            ibanInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
    }

    private boolean isGermanIBAN() {
        return ibanCountryCode.getText().toString().equals("DE");
    }

    private void saveCard() {
        boolean ok = true;

        final String name = nameInput.getText().toString();
        final String iban = ibanCountryCode.getText().toString() + ibanInput.getText().toString().replace(" ", "");

        if(name.length() > 0) {
            nameError.setVisibility(View.INVISIBLE);
        } else {
            nameError.setVisibility(View.VISIBLE);
            shake(nameInput);
            nameInput.setBackgroundResource(R.drawable.ic_round_edittext_error);
            ok = false;
        }

        if(IBAN.validate(iban)) {
            ibanError.setVisibility(View.INVISIBLE);
        } else {
            ibanError.setVisibility(View.VISIBLE);
            shake(ibanInput);
            ibanInput.setBackgroundResource(R.drawable.ic_round_edittext_error);
            ok = false;
        }

        if (ok) {
            SnabbleUI.getUiCallback().requestKeyguard(new KeyguardHandler() {
                @Override
                public void onKeyguardResult(int resultCode) {
                    if (resultCode == Activity.RESULT_OK) {
                        final PaymentCredentials pc = PaymentCredentials.fromSEPA(name, iban);
                        if (pc == null) {
                            Toast.makeText(getContext(), "Could not verify payment credentials", Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            Snabble.getInstance().getPaymentCredentialsStore().add(pc);
                        }

                        if (isShown()) {
                            finish();
                        } else {
                            acceptedKeyguard = true;
                        }
                    }
                }
            });
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

    private void shake(View view) {
        TranslateAnimation shake = new TranslateAnimation(0, 1.5f * getResources().getDisplayMetrics().density, 0, 0);
        shake.setDuration(500);
        shake.setInterpolator(new CycleInterpolator(5));
        view.startAnimation(shake);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

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
                        }
                    }
                }
            };
}
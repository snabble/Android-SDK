package io.snabble.sdk.ui.payment;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.payment.SEPAPaymentCredentials;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;

public class SEPACardInputView extends FrameLayout {
    private Button save;
    private EditText nameInput;
    private EditText ibanInput;
    private View nameError;
    private View ibanError;

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
                    if (((i+2) != 0 && (i+2) % 4 == 0)) {
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

            }

            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void saveCard() {
        boolean ok = true;

        String name = nameInput.getText().toString();
        String iban = "DE" + ibanInput.getText().toString().replace(" ", "");

        if(name.length() > 0) {
            nameError.setVisibility(View.INVISIBLE);
        } else {
            nameError.setVisibility(View.VISIBLE);
            shake(nameInput);
            ok = false;
        }

        if(SEPAPaymentCredentials.validateIBAN(iban)) {
            ibanError.setVisibility(View.INVISIBLE);
        } else {
            ibanError.setVisibility(View.VISIBLE);
            shake(ibanInput);
            ok = false;
        }

        if (ok) {
            SnabbleSdk.getUserPreferences().getPaymentCredentialsStore().add(new SEPAPaymentCredentials(name, iban));

            SnabbleUICallback callback = SnabbleUI.getUiCallback();
            if(callback != null){
                hideSoftKeyboard(ibanInput);
                hideSoftKeyboard(nameInput);
                callback.goBack();
            }
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
}
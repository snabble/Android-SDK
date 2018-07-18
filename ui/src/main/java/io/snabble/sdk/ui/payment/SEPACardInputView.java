package io.snabble.sdk.ui.payment;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.payment.SEPACard;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;

public class SEPACardInputView extends FrameLayout {
    private Button save;
    private EditText ibanInput;
    private EditText bicInput;
    private EditText ownerInput;

    private View ibanError;
    private View bicError;
    private View ownerError;

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

        ibanInput = findViewById(R.id.input_iban);
        bicInput = findViewById(R.id.input_bic);
        ownerInput = findViewById(R.id.input_owner);

        ibanError = findViewById(R.id.input_invalid_iban);
        bicError = findViewById(R.id.input_invalid_bic);
        ownerError = findViewById(R.id.input_invalid_owner);

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
            String oldStr = "";

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString().replace(" ", "");
                StringBuilder sb = new StringBuilder();

                if (isUpdating) {
                    oldStr = str;
                    isUpdating = false;
                    return;
                }

                for (int i = 0; i < str.length(); i++) {
                    if ((i != 0 && i % 4 == 0)) {
                        sb.append(' ');
                    }

                    sb.append(str.charAt(i));
                }

                isUpdating = true;

                String text = sb.toString();
                ibanInput.setText(text);
                ibanInput.setSelection(text.length());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void saveCard() {
        boolean ok = true;

        String owner = ownerInput.getText().toString();

        if(owner.length() > 0){
            ownerError.setVisibility(View.INVISIBLE);
        } else {
            ownerError.setVisibility(View.VISIBLE);
            shake(ownerInput);
            ok = false;
        }

        String iban = ibanInput.getText().toString().replace(" ", "");

        if(SEPACard.validateIBAN(iban)) {
            ibanError.setVisibility(View.INVISIBLE);
        } else {
            ibanError.setVisibility(View.VISIBLE);
            shake(ibanInput);
            ok = false;
        }

        String bic = bicInput.getText().toString();
        if(SEPACard.validateBIC(bic)) {
            bicError.setVisibility(View.INVISIBLE);
        } else {
            bicError.setVisibility(View.VISIBLE);
            shake(bicInput);
            ok = false;
        }

        if (ok) {
            SnabbleSdk sdkInstance = SnabbleUI.getSdkInstance();
            sdkInstance.getUserPreferences().getPaymentCredentialsStore().add(new SEPACard(owner, iban, bic));

            SnabbleUICallback callback = SnabbleUI.getUiCallback();
            if(callback != null){
                callback.goBack();
            }
        }
    }

    private void shake(View view) {
        TranslateAnimation shake = new TranslateAnimation(0, 1.5f * getResources().getDisplayMetrics().density, 0, 0);
        shake.setDuration(500);
        shake.setInterpolator(new CycleInterpolator(5));
        view.startAnimation(shake);
    }
}
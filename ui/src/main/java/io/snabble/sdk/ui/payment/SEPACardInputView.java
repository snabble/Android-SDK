package io.snabble.sdk.ui.payment;

import android.content.Context;
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
    }

    private void saveCard() {
        boolean ok = true;

        String owner = ownerInput.getText().toString();

        if(owner.length() > 0){
            ownerError.setVisibility(View.INVISIBLE);
        } else {
            ownerError.setVisibility(View.VISIBLE);
            ok = false;
        }

        String iban = ibanInput.getText().toString();

        if(SEPACard.validateIBAN(iban)) {
            ibanError.setVisibility(View.INVISIBLE);
        } else {
            ibanError.setVisibility(View.VISIBLE);
            ok = false;
        }

        String bic = bicInput.getText().toString();
        if(SEPACard.validateBIC(bic)) {
            bicError.setVisibility(View.INVISIBLE);
        } else {
            bicError.setVisibility(View.VISIBLE);
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
        TranslateAnimation shake = new TranslateAnimation(0, 10 * getResources().getDisplayMetrics().density, 0, 0);
        shake.setDuration(500);
        shake.setInterpolator(new CycleInterpolator(7));
        view.startAnimation(shake);
    }
}
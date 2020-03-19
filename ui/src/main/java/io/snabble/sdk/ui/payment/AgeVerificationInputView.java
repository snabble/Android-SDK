package io.snabble.sdk.ui.payment;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;

public class AgeVerificationInputView extends FrameLayout {
    public AgeVerificationInputView(@NonNull Context context) {
        super(context);
        inflateView();
    }

    public AgeVerificationInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public AgeVerificationInputView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    public void inflateView() {
        inflate(getContext(), R.layout.snabble_view_age_verification, this);

        TextInputLayout textInputLayout = findViewById(R.id.input_layout);
        TextInputEditText textInputEditText = findViewById(R.id.input);
        Button save = findViewById(R.id.save);

        save.setOnClickListener(view -> {
            Editable editable = textInputEditText.getText();
            if (editable != null) {
                String input = editable.toString();
                if (verify(input)) {
                    Snabble.getInstance().getUserPreferences().setBirthday(getDate(input));
                    SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
                    if (callback != null) {
                        callback.execute(SnabbleUI.Action.GO_BACK, null);
                    }
                } else {
                    textInputLayout.setError(getResources().getString(R.string.Snabble_AgeVerification_errorHint));
                }
            }
        });
    }

    private Date getDate(String input) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMdd");
        try {
            return simpleDateFormat.parse(input.substring(0, 6));
        } catch (ParseException e) {
            return null;
        }
    }

    private boolean verify(String input) {
        if (input.length() != 7) {
            return false;
        }


        int[] multipliers = new int[] { 7, 3, 1, 7, 3, 1} ;
        int checksum = 0;

        int index = 0;
        for (int m : multipliers) {
            int d = Character.digit(input.charAt(index), 10);
            checksum += (d * multipliers[index]) % 10;
            index++;
        }

        int check = checksum % 10;
        return check == Character.digit(input.charAt(6), 10);
    }
}

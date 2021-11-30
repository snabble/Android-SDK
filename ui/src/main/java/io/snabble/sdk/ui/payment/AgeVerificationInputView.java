package io.snabble.sdk.ui.payment;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.Users;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;

public class AgeVerificationInputView extends FrameLayout {
    private DelayedProgressDialog progressDialog;
    private TextInputLayout textInputLayout;
    private TextInputEditText textInputEditText;

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

        setFocusable(true);
        setFocusableInTouchMode(true);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_pleaseWait));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener((dialogInterface, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                Snabble.getInstance().getUsers().cancelUpdatingUser();
                return true;
            }
            return false;
        });

        textInputLayout = findViewById(R.id.input_layout);
        textInputEditText = findViewById(R.id.input);

        textInputEditText.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH
                            || actionId == EditorInfo.IME_ACTION_DONE
                            || event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        handleInput();
                        return true;
                    }
                    return false;
                });

        Button save = findViewById(R.id.save);
        save.setOnClickListener(view -> handleInput());
    }

    private void handleInput() {
        Editable editable = textInputEditText.getText();
        if (editable != null) {
            String input = editable.toString();
            if (verify(input)) {
                hideSoftKeyboard(textInputLayout);
                hideSoftKeyboard(textInputEditText);

                Date birthday = getDate(input);
                Snabble.getInstance().getUsers().setBirthday(birthday, new Users.UpdateUserCallback() {
                    @Override
                    public void success() {
                        post(() -> {
                            Snabble.getInstance().getUserPreferences().setBirthday(getDate(input));
                            SnabbleUI.executeAction(SnabbleUI.Action.GO_BACK);
                        });
                    }

                    @Override
                    public void failure() {
                        post(() -> Toast.makeText(getContext(), R.string.Snabble_networkError, Toast.LENGTH_LONG).show());
                    }
                });
            } else {
                textInputLayout.setError(" ");
            }
        }
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

    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onDetachedFromWindow() {
        hideSoftKeyboard(textInputLayout);
        hideSoftKeyboard(textInputEditText);
        super.onDetachedFromWindow();
    }

}

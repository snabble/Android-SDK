package io.snabble.sdk.ui.payment;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;

public class SEPALegalInfoHelper {
    public static void showSEPALegalInfoIfNeeded(final Context context, final PaymentMethod paymentMethod, final View.OnClickListener clickListener) {
        if (paymentMethod != PaymentMethod.DE_DIRECT_DEBIT) {
            clickListener.onClick(null);
            return;
        }

        Project project = Snabble.getInstance().getCheckedInProject().getValue();

        String shortText = project.getText("sepaMandateShort");
        final String longText = project.getText("sepaMandate");

        if (shortText == null || longText == null) {
            clickListener.onClick(null);
            return;
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.snabble_dialog_sepa_legal_info, null);

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        final TextView message = view.findViewById(R.id.helper_text);
        View ok = view.findViewById(R.id.button);
        View close = view.findViewById(R.id.close);

        int startIndex = shortText.indexOf('*');
        int endIndex = shortText.lastIndexOf('*') - 1;
        shortText = shortText.replace("*", "");
        Spannable spannable = new SpannableString(shortText);

        if (startIndex != -1 && endIndex != -1) {
            int color = UIUtils.getColorByAttribute(context, R.attr.colorPrimary);

            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    message.setText(longText);
                }
            }, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            spannable.setSpan(new ForegroundColorSpan(color), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        message.setText(spannable);
        message.setMovementMethod(LinkMovementMethod.getInstance());

        close.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                alertDialog.dismiss();
            }
        });

        ok.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                alertDialog.dismiss();
                clickListener.onClick(null);
            }
        });

        alertDialog.show();
        alertDialog.getWindow().setGravity(Gravity.BOTTOM);
    }
}

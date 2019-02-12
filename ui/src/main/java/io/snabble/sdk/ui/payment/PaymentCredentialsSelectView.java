package io.snabble.sdk.ui.payment;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import androidx.core.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class PaymentCredentialsSelectView extends FrameLayout {

    private List<Entry> entries;
    private boolean skipOnResume = false;

    public PaymentCredentialsSelectView(Context context) {
        super(context);
        inflateView();
    }

    public PaymentCredentialsSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public PaymentCredentialsSelectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_payment_credentials_select, this);

        entries = new ArrayList<>();
        entries.add(new Entry(R.drawable.ic_sepa_small, "SEPA", new OneShotClickListener() {
            @Override
            public void click() {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    skipOnResume = true;
                    callback.showSEPACardInput();
                }
            }
        }));

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new PaymentCredentialsSelectView.Adapter());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    private static class Entry {
        int drawableRes;
        String text;
        OnClickListener onClickListener;

        Entry(int drawableRes, String text, OnClickListener onClickListener) {
            this.drawableRes = drawableRes;
            this.text = text;
            this.onClickListener = onClickListener;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView icon;

        ViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
            icon = itemView.findViewById(R.id.icon);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item_payment_credentials_select, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            Entry e = entries.get(position);

            holder.icon.setImageResource(e.drawableRes);
            holder.text.setText(e.text);
            holder.itemView.setOnClickListener(e.onClickListener);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
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
                public void onActivityResumed(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        if (skipOnResume) {
                            skipOnResume = false;

                            SnabbleUICallback callback = SnabbleUI.getUiCallback();
                            if (callback != null) {
                                callback.goBack();
                            }
                        }
                    }
                }
            };
}

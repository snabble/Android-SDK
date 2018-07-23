package io.snabble.sdk.ui.scanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import io.snabble.sdk.Product;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.utils.Logger;

class SelectBundleDialog {
    public static void show(Context context, Product product, final Callback callback) {
        if(callback == null) {
            Logger.e("No callback provided");
            return;
        }

        View view = View.inflate(context, R.layout.dialog_bundle_select, null);

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                callback.onDismissed();
            }
        });

        View close = view.findViewById(R.id.close);
        close.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                alertDialog.dismiss();
            }
        });

        ViewGroup container = view.findViewById(R.id.container);

        Product[] bundles = product.getBundleProducts();
        final Product[] products = new Product[bundles.length + 1];
        products[0] = product;
        int i=1;
        for(Product p : bundles) {
            products[i] = p;
            i++;
        }

        for(final Product p : products) {
            View itemView = View.inflate(context, R.layout.item_bundle_select, null);
            TextView name = itemView.findViewById(R.id.name);
            name.setText(p.getName());
            itemView.setOnClickListener(new OneShotClickListener() {
                @Override
                public void click() {
                    alertDialog.dismiss();
                    callback.onProductSelected(p);
                }
            });

            itemView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            container.addView(itemView);
        }

        Window window = alertDialog.getWindow();
        if (window == null) {
            return;
        }

        window.setGravity(Gravity.BOTTOM);
        alertDialog.show();
    }

    public interface Callback {
        void onProductSelected(Product product);
        void onDismissed();
    }
}

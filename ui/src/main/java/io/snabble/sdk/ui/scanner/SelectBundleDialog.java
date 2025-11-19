package io.snabble.sdk.ui.scanner;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.Product;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.utils.OneShotClickListener;
import io.snabble.sdk.utils.Logger;

class SelectBundleDialog {
    public static void show(Context context, Product product, final Callback callback) {
        if (callback == null) {
            Logger.e("No callback provided");
            return;
        }

        View view = View.inflate(context, R.layout.snabble_dialog_bundle_select, null);

        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(true)
                .create();

        alertDialog.setOnDismissListener(dialogInterface -> callback.onDismissed());

        View close = view.findViewById(R.id.close);
        close.setOnClickListener(new OneShotClickListener() {
            @Override
            public void click() {
                alertDialog.dismiss();
            }
        });

        ViewGroup container = view.findViewById(R.id.container);

        Product[] bundles = product.getBundleProducts();
        List<Product> listOfProducts = new ArrayList<>();
        listOfProducts.add(product);

        for (Product p : bundles) {
            if (p.getAvailability() != Product.Availability.NOT_AVAILABLE) {
                listOfProducts.add(p);
            }
        }

        for (final Product p : listOfProducts) {
            View itemView = View.inflate(context, R.layout.snabble_item_bundle_select, null);
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

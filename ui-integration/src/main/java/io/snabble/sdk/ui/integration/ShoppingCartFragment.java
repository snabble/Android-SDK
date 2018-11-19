package io.snabble.sdk.ui.integration;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.cart.ShoppingCartView;

public class ShoppingCartFragment extends Fragment {
    private ShoppingCartView shoppingCartView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shoppingcart, container, false);

        shoppingCartView = v.findViewById(R.id.shopping_cart_view);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_shopping_cart, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            new AlertDialog.Builder(requireContext())
                    .setMessage(R.string.Snabble_Shoppingcart_removeItems)
                    .setPositiveButton(R.string.Snabble_Yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SnabbleUI.getProject().getShoppingCart().clear();
                        }
                    })
                    .setNegativeButton(R.string.Snabble_No, null)
                    .create()
                    .show();
            return true;
        }

        return false;
    }

    public ShoppingCartView getShoppingCartView() {
        return shoppingCartView;
    }
}

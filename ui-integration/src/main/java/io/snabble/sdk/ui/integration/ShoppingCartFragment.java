package io.snabble.sdk.ui.integration;


import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import android.util.AttributeSet;
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
    private int emptyStateLayoutResId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shoppingcart, container, false);

        shoppingCartView = v.findViewById(R.id.shopping_cart_view);
        shoppingCartView.setEmptyStateLayoutResId(emptyStateLayoutResId);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_shopping_cart, menu);
    }

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.ShoppingCartFragment);

        int emptyStateResId = a.getResourceId(R.styleable.ShoppingCartFragment_emptyState, R.layout.view_shopping_cart_empty_state);
        shoppingCartView.setEmptyStateLayoutResId(emptyStateResId);
        a.recycle();
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

package io.snabble.sdk.ui.integration;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class UserPaymentMethodListFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_userpaymentmethod_list, container, false);
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.menu_shopping_cart, menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.action_delete) {
//            new AlertDialog.Builder(requireContext())
//                    .setMessage(R.string.Snabble_Shoppingcart_removeItems)
//                    .setPositiveButton(R.string.Snabble_Yes, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            SnabbleUI.getSdkInstance().getShoppingCart().clear();
//                        }
//                    })
//                    .setNegativeButton(R.string.Snabble_No, null)
//                    .create()
//                    .show();
//            return true;
//        }
//
//        return false;
//    }
}

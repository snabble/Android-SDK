package io.snabble.testapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v =  inflater.inflate(R.layout.fragment_home, container, false);

        v.findViewById(R.id.scanner).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity)getActivity()).showScanner();
            }
        });

        v.findViewById(R.id.cart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity)getActivity()).showShoppingCart();
            }
        });

        v.findViewById(R.id.update_db).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.get().getSnabbleSdk().getProductDatabase().update();
            }
        });

        v.findViewById(R.id.delete_db).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.get().getSnabbleSdk().getProductDatabase().delete();
            }
        });

        v.findViewById(R.id.show_pm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity)getActivity()).showUserPaymentMethodList();
            }
        });
        return v;
    }
}

package io.snabble.testapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
                App.get().getProject().getProductDatabase().update();
            }
        });

        v.findViewById(R.id.delete_db).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.get().getProject().getProductDatabase().delete();
            }
        });

        v.findViewById(R.id.show_pm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity)getActivity()).showPaymentCredentialsList();
            }
        });
        return v;
    }
}

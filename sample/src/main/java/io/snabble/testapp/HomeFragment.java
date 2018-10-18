package io.snabble.testapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.snabble.sdk.ui.receipts.ReceiptListView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.apache.commons.io.FileUtils;

import java.io.IOException;

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
                ((MainActivity)getActivity()).showScanner();
            }
        });

        v.findViewById(R.id.cart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).showShoppingCart();
            }
        });

        v.findViewById(R.id.receipt_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), ReceiptListActivity.class);
                startActivity(intent);
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

        v.findViewById(R.id.clear_cache).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    FileUtils.deleteDirectory(App.get().getCacheDir());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        return v;
    }
}

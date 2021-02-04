package io.snabble.testapp;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.snabble.sdk.Project;
import io.snabble.sdk.Shop;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.SnabbleUI;

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

        v.findViewById(R.id.show_po).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity)getActivity()).showPaymentOptions();
            }
        });

        v.findViewById(R.id.age_verification).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity)getActivity()).showAgeVerification();
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

        v.findViewById(R.id.clear_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityManager activityManager = (ActivityManager) requireContext()
                        .getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.clearApplicationUserData();
            }
        });

        // convenience project switching for debugging - use geofencing or a single project in real apps
        final List<Project> projectList = Snabble.getInstance().getProjects();
        Spinner projects = v.findViewById(R.id.projects);
        projects.setAdapter(new ArrayAdapter<Project>(requireContext(), R.layout.item_dropdown, projectList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setText(projectList.get(position).getId());
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView, parent);
                v.setText(projectList.get(position).getId());
                return v;
            }
        });

        for (int i=0; i<projectList.size(); i++) {
            if (SnabbleUI.getProject() == projectList.get(i)) {
                projects.setSelection(i);
                break;
            }
        }

        projects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Project project = projectList.get(position);

                SnabbleUI.useProject(project);

                updateShops(v);

                if (project.getShops().length > 0) {
                    project.setCheckedInShop(project.getShops()[0]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        updateShops(v);

        return v;
    }

    private void updateShops(View v) {
        Project project = SnabbleUI.getProject();
        final List<Shop> shopList = Arrays.asList(project.getShops());
        Spinner shops = v.findViewById(R.id.shops);
        shops.setAdapter(new ArrayAdapter<Shop>(requireContext(), R.layout.item_dropdown, shopList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                Shop shop = shopList.get(position);
                v.setText(shop.getName() + " (" + shop.getId() + ")");
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView, parent);
                Shop shop = shopList.get(position);
                v.setText(shop.getName() + " (" + shop.getId() + ")");
                return v;
            }
        });

        for (int i=0; i<shopList.size(); i++) {
            if (project.getCheckedInShop() == shopList.get(i)) {
                shops.setSelection(i);
                break;
            }
        }

        shops.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Shop shop = shopList.get(position);
                project.setCheckedInShop(shop);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}

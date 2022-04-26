package io.snabble.testapp;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
import androidx.lifecycle.Observer;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.List;

import io.snabble.sdk.InitializationState;
import io.snabble.sdk.Project;
import io.snabble.sdk.Shop;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.BaseFragment;
import io.snabble.sdk.ui.SnabbleUI;

public class HomeFragment extends BaseFragment {
    private Project currentProject;

    public HomeFragment() {
        super(R.layout.fragment_home, false);
    }

    @Override
    public void onActualViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {

        v.findViewById(R.id.scanner).setOnClickListener(btn -> ((BaseActivity)getActivity()).showScanner());

        v.findViewById(R.id.cart).setOnClickListener(btn -> ((BaseActivity)getActivity()).showShoppingCart());

        v.findViewById(R.id.update_db).setOnClickListener(btn -> App.get().getProject().getProductDatabase().update());

        v.findViewById(R.id.delete_db).setOnClickListener(btn -> App.get().getProject().getProductDatabase().delete());

        v.findViewById(R.id.show_pm).setOnClickListener(btn -> ((BaseActivity)getActivity()).showPaymentCredentialsList());

        v.findViewById(R.id.show_po).setOnClickListener(btn -> ((BaseActivity)getActivity()).showPaymentOptions());

        v.findViewById(R.id.clear_cache).setOnClickListener(btn -> {
            try {
                FileUtils.deleteDirectory(App.get().getCacheDir());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        v.findViewById(R.id.clear_data).setOnClickListener(btn -> {
            ActivityManager activityManager = (ActivityManager) requireContext()
                    .getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.clearApplicationUserData();
        });

        Snabble.getInstance().getInitializationState().observe(getViewLifecycleOwner(), new Observer<InitializationState>() {
            @Override
            public void onChanged(InitializationState initializationState) {
                if (initializationState == InitializationState.INITIALIZED) {
                    // convenience project switching for debugging - use geofencing or a single project in real apps
                    final List<Project> projectList = Snabble.getInstance().getProjects();
                    Spinner projects = v.findViewById(R.id.projects);

                    if (!projectList.isEmpty()) {
                        currentProject = projectList.get(0);
                    }

                    SnabbleUI.getProjectAsLiveData().observe(getViewLifecycleOwner(), project -> {
                        currentProject = project;

                        for (int i = 0; i < projectList.size(); i++) {
                            if (project == projectList.get(i)) {
                                projects.setSelection(i);
                                break;
                            }
                        }

                        updateShops(v);
                    });

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

                    projects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            Project project = projectList.get(position);

                            updateShops(v);

                            if (project.getShops().size() > 0) {
                                Snabble.getInstance().setCheckedInShop(project.getShops().get(0));
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

                    updateShops(v);
                }
            }
        });
    }

    private void updateShops(View v) {
        if (currentProject != null) {
            final List<Shop> shopList = currentProject.getShops();
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

            for (int i = 0; i < shopList.size(); i++) {
                if (Snabble.getInstance().getCheckedInShop() == shopList.get(i)) {
                    Log.d("Restoring", "shop id = " + shopList.get(i).getId());
                    shops.setSelection(i);
                    break;
                }
            }

            shops.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Shop shop = shopList.get(position);
                    Snabble.getInstance().setCheckedInShop(shop);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }
}
package io.snabble.testapp;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.snabble.sdk.Project;
import io.snabble.sdk.ReceiptsApi;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.SnabbleUI;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.util.List;

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

        // convenience project switching for debugging - use geofencing or a single project in real apps
        final List<Project> projectList = Snabble.getInstance().getProjects();
        Spinner projects = v.findViewById(R.id.projects);
        projects.setAdapter(new ArrayAdapter<Project>(requireContext(), R.layout.item_project, projectList) {
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

                if (project.getShops().length > 0) {
                    project.setCheckedInShop(project.getShops()[0]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return v;
    }
}

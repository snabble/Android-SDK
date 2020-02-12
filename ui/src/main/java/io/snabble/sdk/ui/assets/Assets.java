package io.snabble.sdk.ui.assets;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import io.snabble.sdk.utils.Utils;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Assets {
    private enum Variant {
        @SerializedName("1x")
        MDPI(1.0f),
        @SerializedName("1.5x")
        HDPI(1.5f),
        @SerializedName("2x")
        XHDPI(2.0f),
        @SerializedName("3x")
        XXHDPI(3.0f),
        @SerializedName("4x")
        XXXHDPI(4.0f);

        float density;

        Variant(float density) {
            this.density = density;
        }
    }

    private class ApiAsset {
        String name;
        Map<Variant, String> variants;
    }

    private class ApiManifest {
        ApiAsset[] files;
    }

    public class Asset {
        public File asset;
        public float density;

        public Asset(File asset, float density) {
            this.asset = asset;
            this.density = density;
        }

        public void loadInto(ImageView imageView) {

        }
    }

    public interface Callback {
        void onReceive(Bitmap bitmap);
    }

    private OkHttpClient okHttpClient;
    private File assetDir;
    private Project project;
    private SharedPreferences sharedPreferences;

    public Assets(Project project) {
        this.project = project;
        this.sharedPreferences = Snabble.getInstance().getApplication()
                .getSharedPreferences("snabble_assets", Context.MODE_PRIVATE);
        this.okHttpClient = project.getOkHttpClient()
                .newBuilder()
                .cache(null)
                .build();
        this.assetDir = new File(project.getInternalStorageDirectory(), "assets/");
    }

    public void download(Project project) {
        Request request = new Request.Builder()
                .cacheControl(CacheControl.parse(Headers.of("max-age=30")))
                .url(project.getAssetsUrl())
                .get()
                .build();

        project.getOkHttpClient().newCall(request).enqueue(new SimpleJsonCallback<ApiManifest>(ApiManifest.class) {
            @Override
            public void success(ApiManifest manifest) {
                downloadAssets(project, manifest);
            }

            @Override
            public void error(Throwable t) {

            }
        });
    }

    private File getLocalAssetFile(String hash) {
        return new File(assetDir, hash);
    }

    private void downloadAssets(Project project, ApiManifest manifest) {
        if (manifest.files != null) {
            SharedPreferences.Editor prefs = sharedPreferences.edit();
            Set<String> hashes = new HashSet<>();
            Variant bestVariant = getBestVariant();

            for (ApiAsset apiAsset : manifest.files) {
                String url = apiAsset.variants.get(bestVariant);
                String hash = Utils.sha1Hex(url);
                hashes.add(hash);

                File localFile = getLocalAssetFile(hash);
                if (localFile.exists()) {
                    continue;
                }

                if (url != null) {
                    Request request = new Request.Builder()
                            .url(Snabble.getInstance().absoluteUrl(url))
                            .get()
                            .build();

                    project.getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                ResponseBody body = response.body();
                                if (body != null) {
                                    if (localFile.createNewFile()) {
                                        Asset asset = new Asset(localFile, bestVariant.density);
                                        IOUtils.copy(body.byteStream(), new FileOutputStream(localFile));
                                        prefs.putString(apiAsset.name, GsonHolder.get().toJson(asset));
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call call, IOException e) {

                        }
                    });
                }
            }

            Map<String, ?> all = sharedPreferences.getAll();
            for (
            // clear orphans
            File[] files = assetDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!hashes.contains(f.getName())) {
                        f.delete();
                    }
                }
            }
        } else {
            Logger.e("Unknown asset manifest file format, ignoring.");
        }
    }

    public Variant getBestVariant() {
        Resources res = Snabble.getInstance().getApplication().getResources();
        float density = res.getDisplayMetrics().density;

        float bestDiff = Float.MAX_VALUE;
        Variant bestVariant = Variant.XXHDPI;

        for (Variant variant : Variant.values()) {
            float diff = Math.abs(density - variant.density);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestVariant = variant;
            }
        }

        return bestVariant;
    }

    public void get(String id, Callback callback) {
        try {
            download(SnabbleUI.getProject());
        } catch (Exception e) {
            callback.onReceive(null);
        }
    }

    /{projectId}/assets/manifest.json
}

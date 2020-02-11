package io.snabble.sdk.ui.assets;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Map;

import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

    private class Asset {
        String name;
        Map<Variant, String> variants;
    }

    private class Manifest {
        Asset[] files;
    }

    public interface Callback {
        void onReceive(Bitmap bitmap);
    }

    public void download(Project project) {
        Request request = new Request.Builder()
                .cacheControl(CacheControl.parse(Headers.of("max-age=30")))
                .url(project.getAssetsUrl())
                .get()
                .build();

        project.getOkHttpClient().newCall(request).enqueue(new SimpleJsonCallback<Manifest>(Manifest.class) {
            @Override
            public void success(Manifest manifest) {
                downloadAssets(project, manifest);
            }

            @Override
            public void error(Throwable t) {

            }
        });
    }

    private void downloadAssets(Project project, Manifest manifest) {
        Resources res = Snabble.getInstance().getApplication().getResources();

        OkHttpClient okHttpClient = project.getOkHttpClient()
                .newBuilder()
                .cache(null)
                .build();

        Variant bestVariant = getBestVariant();

        if (manifest.files != null) {
            for (Asset asset : manifest.files) {
                String url = asset.variants.get(bestVariant);

                if (url != null) {
                    Request request = new Request.Builder()
                            .url(Snabble.getInstance().absoluteUrl(url))
                            .get()
                            .build();

                    project.getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                AssetManager assetManager =
                                response.body().byteStream()
                            }
                        }

                        @Override
                        public void onFailure(Call call, IOException e) {

                        }
                    });
                }
            }
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

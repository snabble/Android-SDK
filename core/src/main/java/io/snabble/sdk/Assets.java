package io.snabble.sdk;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import io.snabble.sdk.utils.Utils;
import okhttp3.CacheControl;
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
        public File file;
        public float density;
        public String hash;

        public Asset(File file, float density, String hash) {
            this.file = file;
            this.density = density;
            this.hash = hash;
        }

        public void loadInto(ImageView imageView) {

        }
    }

    private interface DownloadCallback {
        void success();
        void failure();
    }

    public interface Callback {
        void onReceive(Bitmap bitmap);
    }

    private final Object lock = new Object();

    private Application app;
    private File assetDir;
    private Project project;
    private SharedPreferences sharedPreferences;
    private Handler mainThreadHandler;

    Assets(Project project) {
        this.app = Snabble.getInstance().getApplication();
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
        this.project = project;
        this.sharedPreferences = app.getSharedPreferences("snabble_assets", Context.MODE_PRIVATE);
        this.assetDir = new File(project.getInternalStorageDirectory(), "assets/");
        this.assetDir.mkdirs();
    }

    public void update() {
        download(null);
    }

    private void download(DownloadCallback callback) {
        mainThreadHandler.post(() -> {
            synchronized (lock) {
                Request request = new Request.Builder()
                        .cacheControl(new CacheControl.Builder()
                                .maxAge(30, TimeUnit.SECONDS)
                                .build())
                        .url(project.getAssetsUrl())
                        .get()
                        .build();

                project.getOkHttpClient().newCall(request).enqueue(new SimpleJsonCallback<ApiManifest>(ApiManifest.class) {
                    @Override
                    public void success(ApiManifest manifest) {
                        downloadAssets(project, manifest, callback);
                    }

                    @Override
                    public void error(Throwable t) {
                        if (callback != null) {
                            callback.failure();
                        }
                    }
                });
            }
        });
    }

    private File getLocalAssetFile(String hash) {
        return new File(assetDir, hash);
    }

    @SuppressLint("ApplySharedPref")
    private void downloadAssets(Project project, ApiManifest manifest, DownloadCallback callback) {
        if (manifest.files != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Set<String> hashes = new HashSet<>();
            Variant bestVariant = getBestVariant();

            for (ApiAsset apiAsset : manifest.files) {
                String url = apiAsset.variants.get(bestVariant);
                if (url == null) {
                    continue;
                }

                String hash = Utils.sha1Hex(url);
                hashes.add(hash);

                if (!FilenameUtils.getExtension(apiAsset.name).equals("png")) {
                    continue;
                }

                File localFile = getLocalAssetFile(hash);
                if (localFile.exists()) {
                    continue;
                }

                Request request = new Request.Builder()
                        .url(Snabble.getInstance().absoluteUrl(url))
                        .cacheControl(new CacheControl.Builder()
                                .noCache()
                                .noStore()
                                .build())
                        .get()
                        .build();

                try {
                    Logger.d("download " + apiAsset.name);
                    Response response = project.getOkHttpClient().newCall(request).execute();

                    if (response.isSuccessful()) {
                        ResponseBody body = response.body();
                        if (body != null) {
                            if (localFile.createNewFile()) {
                                Logger.d("add " + apiAsset.name);

                                Asset asset = new Asset(localFile, bestVariant.density, hash);
                                IOUtils.copy(body.byteStream(), new FileOutputStream(localFile));
                                editor.putString(apiAsset.name, GsonHolder.get().toJson(asset));
                            }
                        }
                    }
                } catch (IOException e) {
                    // ignore
                }
            }

            mainThreadHandler.post(() -> {
                Map<String, ?> all = sharedPreferences.getAll();
                for (Map.Entry<String, ?> entry : all.entrySet()) {
                    Asset asset = GsonHolder.get().fromJson((String) entry.getValue(), Asset.class);
                    if (!hashes.contains(asset.hash)) {
                        Logger.d("remove " + entry.getKey());

                        asset.file.delete();
                        editor.remove(entry.getKey());
                    }
                }

                editor.commit();
            });

            if (callback != null) {
                callback.success();
            }
        } else {
            Logger.e("Unknown file manifest file format, ignoring.");

            if (callback != null) {
                callback.failure();
            }
        }
    }

    private Variant getBestVariant() {
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

    private Bitmap getBitmap(String name) {
        String json = sharedPreferences.getString(name, null);
        if (json != null) {
            try {
                Logger.d("decode " + name);
                Resources res = app.getResources();
                DisplayMetrics dm = res.getDisplayMetrics();

                Asset asset = GsonHolder.get().fromJson(json, Asset.class);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(asset.file), null, options);
                if (bitmap == null || dm.density == asset.density) {
                    return bitmap;
                } else {
                    float scaleFactor = dm.density / asset.density;
                    int w = Math.round(bitmap.getWidth() * scaleFactor);
                    int h = Math.round(bitmap.getHeight() * scaleFactor);

                    Logger.d("rescaling image " + name + " sourceDensity " + asset.density + " targetDensity " + dm.density);
                    return Bitmap.createScaledBitmap(bitmap, w, h, true);
                }
            } catch (Exception e) {
                Logger.d("could not decode " + name + ": " + e.toString());
                return null;
            }
        }

        return null;
    }

    public void get(String name, Callback callback) {
        final String fileName = FilenameUtils.removeExtension(name) + ".png";

        Bitmap bitmap = getBitmap(fileName);
        if (bitmap != null) {
            Logger.d("cache hit " + fileName);
            callback.onReceive(bitmap);
        } else {
            Logger.d("cache miss " + fileName);
            download(new DownloadCallback() {
                @Override
                public void success() {
                    mainThreadHandler.post(() -> callback.onReceive(getBitmap(fileName)));
                }

                @Override
                public void failure() {
                    Logger.d("fail " + fileName);
                    mainThreadHandler.post(() -> callback.onReceive(null));
                }
            });
        }
    }
}

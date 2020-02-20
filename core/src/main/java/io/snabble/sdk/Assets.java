package io.snabble.sdk;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.util.DisplayMetrics;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.utils.Dispatch;
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

    private class Asset {
        public File file;
        public float density;
        public String hash;

        public Asset(File file, float density, String hash) {
            this.file = file;
            this.density = density;
            this.hash = hash;
        }
    }

    private class Manifest {
        Map<String, Asset> assets;
    }

    private interface DownloadCallback {
        void success();
        void failure();
    }

    public interface Callback {
        void onReceive(Bitmap bitmap);
    }

    private Application app;
    private File assetDir;
    private Project project;
    private File manifestFile;
    private Manifest manifest;

    Assets(Project project) {
        this.app = Snabble.getInstance().getApplication();
        this.project = project;
        this.manifestFile = new File(project.getInternalStorageDirectory(), "assets.json");
        this.assetDir = new File(project.getInternalStorageDirectory(), "assets/");
        this.assetDir.mkdirs();

        loadManifest();
    }

    public void update() {
        download(null);
    }

    private void loadManifest() {
        Logger.d("Load manifest for project %s", project.getId());

        Dispatch.background(() -> {
            try {
                FileReader fileReader = new FileReader(manifestFile);
                Manifest newManifest = GsonHolder.get().fromJson(fileReader, Manifest.class);

                Dispatch.mainThread(() -> {
                    if (newManifest != null && newManifest.assets != null) {
                        manifest = newManifest;
                    } else {
                        manifest = new Manifest();
                        manifest.assets = new HashMap<>();
                    }
                });
                fileReader.close();
            } catch (IOException e) {
                Logger.d("No assets for project %s", project.getId());

                Dispatch.mainThread(() -> {
                    manifest = new Manifest();
                    manifest.assets = new HashMap<>();
                });
            }
        });
    }

    private void saveManifest() {
        Logger.d("Save manifest for project %s", project.getId());

        Dispatch.background(() -> {
            try {
                String[] json = new String[1];

                CountDownLatch countDownLatch = new CountDownLatch(1);
                Dispatch.mainThread(() -> {
                    json[0] = GsonHolder.get().toJson(manifest);
                    countDownLatch.countDown();
                });
                countDownLatch.await();

                FileWriter fileWriter = new FileWriter(manifestFile);
                fileWriter.write(json[0]);
                fileWriter.close();
            } catch (IOException e) {
                Logger.e("Could not write assets: " + e.getMessage());
            } catch (InterruptedException e) {
                Logger.e(e.getMessage());
            }
        });
    }

    private void download(DownloadCallback callback) {
        Dispatch.mainThread(() -> {
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
        });
    }

    private File getLocalAssetFile(String hash) {
        return new File(assetDir, hash);
    }

    @SuppressLint("ApplySharedPref")
    private void downloadAssets(Project project, ApiManifest apiManifest, DownloadCallback callback) {
        if (apiManifest.files != null) {
            boolean[] changed = new boolean[1];

            Set<String> hashes = new HashSet<>();
            Variant bestVariant = getBestVariant();

            for (ApiAsset apiAsset : apiManifest.files) {
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

                                Dispatch.mainThread(() -> {
                                    manifest.assets.put(apiAsset.name, asset);
                                    changed[0] = true;
                                });
                            }
                        }
                    }
                } catch (IOException e) {
                    Logger.e(e.getMessage());
                }
            }

            Dispatch.mainThread(() -> {
                ArrayList<String> removals = new ArrayList<>();

                if (manifest != null) {
                    for (Map.Entry<String, Asset> entry : manifest.assets.entrySet()) {
                        Asset asset = entry.getValue();
                        if (!hashes.contains(asset.hash)) {
                            Logger.d("remove " + entry.getKey());

                            asset.file.delete();
                            removals.add(entry.getKey());
                        }
                    }

                    for (String s : removals) {
                        manifest.assets.remove(s);
                    }
                }

                if (changed[0] || removals.size() > 0) {
                    saveManifest();
                }
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
        // currently a limitation due to asynchronous reads and saves,
        // could be solved with careful locking
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("Can only be access from the main thread");
        }

        if (manifest == null) {
            return null;
        }

        Asset asset = manifest.assets.get(name);
        if (asset != null) {
            try {
                Logger.d("decode " + name);
                Resources res = app.getResources();
                DisplayMetrics dm = res.getDisplayMetrics();

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
                    Dispatch.mainThread(() -> callback.onReceive(getBitmap(fileName)));
                }

                @Override
                public void failure() {
                    Logger.d("fail " + fileName);
                    Dispatch.mainThread(() -> callback.onReceive(null));
                }
            });
        }
    }
}

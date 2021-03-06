package io.snabble.sdk;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Looper;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatDelegate;

import com.caverock.androidsvg.SVG;
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
        MDPI("1x", 1.0f),
        @SerializedName("1.5x")
        HDPI("1.5x", 1.5f),
        @SerializedName("2x")
        XHDPI("2x", 2.0f),
        @SerializedName("3x")
        XXHDPI("3x", 3.0f),
        @SerializedName("4x")
        XXXHDPI("4x", 4.0f);

        float density;
        String tag;

        Variant(String tag, float density) {
            this.density = density;
            this.tag = tag;
        }
    }

    public enum Type {
        SVG,
        JPG,
        WEBP
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
        public String hash;

        public Asset(File file, String hash) {
            this.file = file;
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
        download(null, null);
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

    private void download(String nonRootIncludedFileName, DownloadCallback callback) {
        Dispatch.mainThread(() -> {
            if (project.getAssetsUrl() == null) {
                if (callback != null) {
                    callback.failure();
                }
                return;
            }

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
                    downloadAssets(project, manifest, nonRootIncludedFileName, callback);
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
    private void downloadAssets(Project project, ApiManifest apiManifest, String nonRootIncludedFileName, DownloadCallback callback) {
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

                String ext = FilenameUtils.getExtension(apiAsset.name);
                if (!ext.equals("svg") && !ext.equals("jpg") && !ext.equals("webp")) {
                    continue;
                }

                // exclude assets that are not in the root when not explicitly requested
                if (apiAsset.name.contains("/") && !apiAsset.name.equals(nonRootIncludedFileName)) {
                    continue;
                }

                if (manifest.assets.containsKey(apiAsset.name)) {
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
                            File localFile = getLocalAssetFile(hash);
                            localFile.createNewFile();

                            Logger.d("add " + apiAsset.name);

                            Asset asset = new Asset(localFile, hash);
                            IOUtils.copy(body.byteStream(), new FileOutputStream(localFile));

                            Dispatch.mainThread(() -> {
                                manifest.assets.put(apiAsset.name, asset);
                                changed[0] = true;
                            });
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
            Logger.e("Unknown manifest file format, ignoring.");

            if (callback != null) {
                callback.failure();
            }
        }
    }

    private Variant getBestVariant() {
        return Variant.MDPI;
    }

    private static boolean isNightModeActive(Context context) {
        int defaultNightMode = AppCompatDelegate.getDefaultNightMode();
        if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        }
        if (defaultNightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        }

        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                return false;
            case Configuration.UI_MODE_NIGHT_YES:
                return true;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                return false;
        }
        return false;
    }

    public Bitmap getBitmap(String name) {
        return getBitmapSVG(name);
    }

    public Bitmap getBitmap(String name, Type type) {
        switch (type) {
            case SVG:
                return getBitmapSVG(name);
            case JPG:
            case WEBP:
                return getBitmapByType(name, type);
            default:
                return null;
        }
    }

    private Asset getAsset(String name, Type type) {
        // currently a limitation due to asynchronous reads and saves,
        // could be solved with careful locking
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("Can only be access from the main thread");
        }

        if (manifest == null) {
            return null;
        }

        boolean nightMode = isNightModeActive(Snabble.getInstance().getApplication());

        String fileName = FilenameUtils.removeExtension(name) + (nightMode ? "_dark" : "");

        switch (type) {
            case SVG:
                fileName += ".svg";
                break;
            case JPG:
                fileName += ".jpg";
                break;
            case WEBP:
                fileName += ".webp";
                break;
        }

        Asset asset = manifest.assets.get(fileName);
        if (asset == null) {
            // try non night mode version
            asset = manifest.assets.get(name);
        }

        return asset;
    }

    private Bitmap getBitmapByType(String name, Type type) {
        Asset asset = getAsset(name, type);
        if (asset != null) {
            try {
                Logger.d("render jpg %s/%s", project.getId(), name);
                return BitmapFactory.decodeStream(new FileInputStream(asset.file));
            } catch (Exception e) {
                Logger.d("could not decode " + name + ": " + e.toString());
                return null;
            }
        }

        return null;
    }

    private Bitmap getBitmapSVG(String name) {
        Asset asset = getAsset(name, Type.SVG);
        if (asset != null) {
            try {
                Logger.d("render svg %s/%s", project.getId(), name);

                Resources res = app.getResources();
                DisplayMetrics dm = res.getDisplayMetrics();

                SVG svg = SVG.getFromInputStream(new FileInputStream(asset.file));
                int width = Math.round(svg.getDocumentWidth() * dm.density);
                int height = Math.round(svg.getDocumentHeight() * dm.density);

                svg.setDocumentWidth(width);
                svg.setDocumentHeight(height);

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                svg.renderToCanvas(canvas);
                return bitmap;
            } catch (Exception e) {
                Logger.d("could not decode " + name + ": " + e.toString());
                return null;
            }
        }

        return null;
    }

    public void get(String name, Callback callback) {
        get(name, Type.SVG, callback);
    }

    public void get(String name, Type type, Callback callback) {
        String fileName = FilenameUtils.removeExtension(name);
        switch (type) {
            case SVG:
                fileName += ".svg";
                break;
            case JPG:
                fileName += ".jpg";
                break;
            case WEBP:
                fileName += ".webp";
                break;
        }
        final String finalFileName = fileName;

        Bitmap bitmap = getBitmap(fileName, type);
        if (bitmap != null) {
            Logger.d("cache hit " + fileName);
            callback.onReceive(bitmap);
        } else {
            Logger.d("cache miss " + fileName);

            download(fileName, new DownloadCallback() {
                @Override
                public void success() {
                    Dispatch.mainThread(() -> callback.onReceive(getBitmap(finalFileName, type)));
                }

                @Override
                public void failure() {
                    Logger.d("fail " + finalFileName);
                    Dispatch.mainThread(() -> callback.onReceive(null));
                }
            });
        }
    }
}

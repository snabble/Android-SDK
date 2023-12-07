package io.snabble.sdk.utils;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import okhttp3.OkHttpClient;
import okhttp3.Response;

public abstract class StringDownloader extends Downloader {
    private File storageFile = null;
    private String assetPath;
    private int rawResId;
    private Application context;
    private final Charset utf8 = Charset.forName("UTF-8");

    public StringDownloader(OkHttpClient okHttpClient) {
        super(okHttpClient);
    }

    /**
     * Use bundled data that are located in the assets folder, paths are relative.
     * <p>
     * Copies the bundled data from the asset folder into the app internal files folder and
     * uses the app files data unless the app gets updated.
     */
    public void setBundledData(Application context, String assetPath, int rawResId, File storageFile) {
        this.context = context;
        this.storageFile = storageFile;
        this.assetPath = assetPath;
        this.rawResId = rawResId;

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            //you can not check for file modified time in the android assets folder,
            //so we check for the install time of the app
            //this implicitly means that while developing you always start with the seeded data
            //and that app updates should always contain updated seeds
            if(storageFile.exists() && storageFile.lastModified() > packageInfo.lastUpdateTime) {
                loadFromSavedData();
            } else {
                Logger.d("Using initial seed: %s", storageFile.getAbsolutePath());
                loadFromBundledData();
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            Logger.e("Could not load saved data: %s", storageFile.getAbsolutePath());
            loadFromBundledData();
        }
    }

    private void loadFromSavedData() throws IOException {
        FileInputStream fis = new FileInputStream(storageFile);
        Logger.d("Using saved data: %s", storageFile.getAbsolutePath());
        String savedData = IOUtils.toString(fis, utf8);
        if(savedData != null && savedData.length() > 0){
            onDownloadFinished(savedData);
        } else {
            Logger.d("Data corrupted, using initial seed: %s", storageFile.getAbsolutePath());
            loadFromBundledData();
        }
        fis.close();
    }

    private void loadFromBundledData() {
        try {
            if(rawResId != 0) {
                try {
                    InputStream bundledDataInputStream = context.getResources().openRawResource(rawResId);
                    onDownloadFinished(IOUtils.toString(bundledDataInputStream, utf8));
                    bundledDataInputStream.close();
                    return;
                } catch (Resources.NotFoundException e) {
                    Logger.e("Could not load bundled data from resources: %s", e.toString());
                }
            }
            if(assetPath != null) {
                InputStream bundledDataInputStream = context.getAssets().open(assetPath);
                onDownloadFinished(IOUtils.toString(bundledDataInputStream, utf8));
                bundledDataInputStream.close();
            }
        } catch (IOException e) {
            Logger.e("Could not load bundled data: %s", e.toString());
        }
    }

    public void setStorageFile(File storageFile) {
        this.storageFile = storageFile;

        if (storageFile.exists()) {
            try {
                loadFromSavedData();
            } catch (IOException e) {
                Logger.e("Could not load saved data: %s", e.toString());
            }
        }
    }

    /**
     * Updates the bundled data that was used in {@link StringDownloader#setBundledData}.
     * <p>
     * Note that app updates are causing the updated data written here to be overwritten again,
     * as it is assumed that new builds always have newer data.
     */
    public synchronized void updateStorage(String content) {
        if (storageFile != null) {
            try {
                File tempFile = new File(FilenameUtils.getFullPath(storageFile.getPath())
                        + FilenameUtils.getBaseName(storageFile.getName())
                        + "_temp." + FilenameUtils.getExtension(storageFile.getName()));

                FileOutputStream fos = new FileOutputStream(tempFile);
                IOUtils.write(content, fos, utf8);
                fos.close();

                FileUtils.deleteQuietly(storageFile);
                FileUtils.moveFile(tempFile, storageFile);
                Logger.d("Updated saved data:%s", storageFile.getAbsolutePath());
            } catch (IOException e) {
                Logger.e("Could not update saved data %s", e.toString());
            }
        }
    }

    @Override
    protected void onResponse(Response response) throws IOException {
        Logger.d("Receiving data for %s...", getUrl());

        onDownloadFinished(response.body().string());
        Logger.d("Received data for %s", getUrl());
    }

    protected abstract void onDownloadFinished(String string);
}

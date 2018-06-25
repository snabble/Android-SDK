package io.snabble.sdk.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class StringDownloader extends Downloader {
    private File storageFile = null;

    public StringDownloader(OkHttpClient okHttpClient) {
        super(okHttpClient);
    }

    /**
     * Use bundled data that are located in the assets folder, paths are relative.
     * <p>
     * Copies the bundled data from the asset folder into the app internal files folder and
     * uses the app files data unless the app gets updated.
     */
    public void setBundledData(Context context, String assetPath, File storageFile) {
        setStorageFile(storageFile);

        try {
            InputStream bundledDataInputStream = context.getAssets().open(assetPath);
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            //you can not check for file modified time in the android assets folder,
            //so we check for the install time of the app
            //this implicitly means that while developing you always start with the seeded data
            //and that app updates should always contain updated seeds
            if(storageFile.exists() && storageFile.lastModified() > packageInfo.lastUpdateTime) {
                loadFromSavedData();
            } else {
                Logger.d("Using initial seed: %s", storageFile.getAbsolutePath());
                onDownloadFinished(IOUtils.toString(bundledDataInputStream, Charset.forName("UTF-8")));
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            Logger.e("Could not load saved data: %s", storageFile.getAbsolutePath());
        }
    }

    private void loadFromSavedData() throws IOException {
        FileInputStream fis = new FileInputStream(storageFile);
        Logger.d("Using saved data: %s", storageFile.getAbsolutePath());
        onDownloadFinished(IOUtils.toString(fis, Charset.forName("UTF-8")));
        fis.close();
    }

    public void setStorageFile(File storageFile) {
        this.storageFile = storageFile;

        if (storageFile.exists()) {
            try {
                loadFromSavedData();
            } catch (IOException e) {
                Logger.e("Could not load saved data: %s", storageFile.getAbsolutePath());
            }
        }
    }

    /**
     * Updates the bundled data that was used in {@link StringDownloader#setBundledData}.
     * <p>
     * Note that app updates are causing the updated data written here to be overwritten again,
     * as it is assumed that new builds always have newer data.
     */
    public void updateStorage(String content) {
        if (storageFile != null) {
            FileUtils.deleteQuietly(storageFile);

            try {
                FileOutputStream fos = new FileOutputStream(storageFile);
                IOUtils.write(content, fos, Charset.forName("UTF-8"));
                fos.close();
                Logger.d("Updated saved data:%s", storageFile.getAbsolutePath());
            } catch (IOException e) {
                Logger.e("Could not update saved data %s", storageFile.getAbsolutePath());
            }
        }
    }

    @Override
    protected void onResponse(Response response) throws IOException {
        Logger.d("Receiving data for %s...", getUrl());
        ResponseBody body = response.body();
        if (body != null) {
            onDownloadFinished(body.string());
            Logger.d("Received data for %s", getUrl());
        }
    }

    protected abstract void onDownloadFinished(String string);
}

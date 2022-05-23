package io.snabble.sdk;

import com.google.gson.JsonObject;

import java.io.File;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.StringDownloader;
import okhttp3.OkHttpClient;

class MetadataDownloader extends StringDownloader {
    private boolean hasData = false;
    private JsonObject jsonObject;

    public MetadataDownloader(OkHttpClient okHttpClient, String bundledFileAssetPath) {
        super(okHttpClient);

        File storageFile = new File(Snabble.getInstance().getInternalStorageDirectory(), "metadata_v2.json");

        if (bundledFileAssetPath != null) {
            setBundledData(Snabble.getInstance().getApplication(), bundledFileAssetPath, storageFile);
        } else {
            setStorageFile(storageFile);
        }

        setUrl(Snabble.getInstance().getMetadataUrl());
    }

    @Override
    protected synchronized void onDownloadFinished(String content) {
        try {
            updateStorage(content);
            jsonObject = GsonHolder.get().fromJson(content, JsonObject.class);
            hasData = true;
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }

    public boolean hasData() {
        return hasData;
    }
}

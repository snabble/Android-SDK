package io.snabble.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.StringDownloader;

class MetadataDownloader extends StringDownloader {
    private boolean hasData = false;
    private JsonObject jsonObject;
    private Gson gson;

    public MetadataDownloader(String bundledFileAssetPath) {
        super(Snabble.getInstance().getOkHttpClient());

        File storageFile = new File(Snabble.getInstance().getInternalStorageDirectory(), "metadata.json");

        if (bundledFileAssetPath != null) {
            setBundledData(Snabble.getInstance().getApplication(), bundledFileAssetPath, storageFile);
        } else {
            setStorageFile(storageFile);
        }

        gson = new Gson();

        setUrl(Snabble.getInstance().getMetadataUrl());
    }

    @Override
    protected synchronized void onDownloadFinished(String content) {
        try {
            updateStorage(content);
            jsonObject = gson.fromJson(content, JsonObject.class);
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

package io.snabble.sdk;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.StringDownloader;

class MetadataDownloader extends StringDownloader {
    private SnabbleSdk sdk;

    private Map<String, String> urls = Collections.unmodifiableMap(new HashMap<String, String>());
    private Map<String, String> extras = Collections.unmodifiableMap(new HashMap<String, String>());
    private Bundle flags = new Bundle();

    public MetadataDownloader(SnabbleSdk sdk,
                              String bundledFileAssetPath) {
        super(sdk.getOkHttpClient());

        this.sdk = sdk;

        if (bundledFileAssetPath != null) {
            setBundledData(sdk.getApplication(), bundledFileAssetPath,
                    new File(sdk.getInternalStorageDirectory(), "metadata.json"));
        }

        setUrl(sdk.getMetadataUrl());
    }

    @Override
    protected void onDownloadFinished(String content) {
        try {
            Map<String, String> urls = new HashMap<>();
            Map<String, String> extras = new HashMap<>();
            Bundle flags = new Bundle();

            JSONObject jsonObject = new JSONObject(content);

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                if (!k.equals("links")) {
                    extras.put(k, jsonObject.getString(k));
                }
            }

            JSONObject links = jsonObject.getJSONObject("links");
            Iterator<String> linkKeys = links.keys();
            while (linkKeys.hasNext()) {
                String k = linkKeys.next();
                urls.put(k, sdk.absoluteUrl(links.getJSONObject(k).getString("href")));
            }

            JSONObject metadata = jsonObject.getJSONObject("metadata");
            Iterator<String> metadataKeys = metadata.keys();
            while (metadataKeys.hasNext()) {
                String k = metadataKeys.next();
                Object object = metadata.get(k);
                putObject(flags, k, object);
            }

            this.urls = Collections.unmodifiableMap(urls);
            this.extras = Collections.unmodifiableMap(extras);
            this.flags = flags;

            updateStorage(content);
        } catch (JSONException e) {
            Logger.e(e.getMessage());
        }
    }

    private void putObject(Bundle bundle, String key, Object value) {
        if (value instanceof Integer) {
            bundle.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            bundle.putLong(key, (Long) value);
        } else if (value instanceof Boolean) {
            bundle.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            bundle.putFloat(key, (Float) value);
        } else if (value instanceof Double) {
            bundle.putDouble(key, (Double) value);
        } else if (value instanceof String) {
            bundle.putString(key, (String) value);
        }
    }

    public Map<String, String> getUrls() {
        return urls;
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    public Bundle getFlags() {
        return flags;
    }
}

package io.snabble.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.snabble.sdk.utils.JsonUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.StringDownloader;

class MetadataDownloader extends StringDownloader {
    private SnabbleSdk sdk;

    private Map<String, String> urls = Collections.unmodifiableMap(new HashMap<String, String>());
    private Map<String, String> extras = Collections.unmodifiableMap(new HashMap<String, String>());
    private JsonObject metadata;
    private JsonObject project;

    private boolean hasData = false;
    private RoundingMode roundingMode;
    private boolean verifyInternalEanChecksum;

    public MetadataDownloader(SnabbleSdk sdk,
                              String bundledFileAssetPath) {
        super(sdk.getOkHttpClient());

        this.sdk = sdk;
        File storageFile = new File(sdk.getInternalStorageDirectory(), "metadata.json");

        if (bundledFileAssetPath != null) {
            setBundledData(sdk.getApplication(), bundledFileAssetPath, storageFile);
        } else {
            setStorageFile(storageFile);
        }

        setUrl(sdk.getMetadataUrl());
    }

    @Override
    protected void onDownloadFinished(String content) {
        try {
            Map<String, String> urls = new HashMap<>();
            Map<String, String> extras = new HashMap<>();

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);

            Set<String> keys = jsonObject.keySet();
            for (String k : keys) {
                if (!k.equals("links")) {
                    extras.put(k, gson.toJson(jsonObject.get(k)));
                }
            }

            JsonObject links = jsonObject.get("links").getAsJsonObject();
            Set<String> linkKeys = links.keySet();
            for (String k : linkKeys) {
                urls.put(k, sdk.absoluteUrl(links.get(k).getAsJsonObject().get("href").getAsString()));
            }

            if (jsonObject.has("metadata")) {
                this.metadata = jsonObject.get("metadata").getAsJsonObject();

            }

            if (jsonObject.has("project")) {
                this.project = jsonObject.get("project").getAsJsonObject();
                this.roundingMode = parseRoundingMode(project.get("roundingMode"));
                this.verifyInternalEanChecksum = JsonUtils.getBooleanOpt(project, "verifyInternalEanChecksum", true);
            }

            this.urls = Collections.unmodifiableMap(urls);
            this.extras = Collections.unmodifiableMap(extras);

            updateStorage(content);
            hasData = true;
        } catch (JsonSyntaxException e){
            Logger.e(e.getMessage());
        }
    }

    public RoundingMode parseRoundingMode(JsonElement jsonElement){
        if(jsonElement != null){
            String roundingMode = jsonElement.getAsString();
            if(roundingMode != null){
                switch(roundingMode){
                    case "up":
                        return RoundingMode.UP;
                    case "down":
                        return RoundingMode.DOWN;
                    case "commercial":
                        return RoundingMode.HALF_UP;
                }
            }
        }

        return RoundingMode.HALF_UP;
    }

    public boolean hasData() {
        return hasData;
    }

    public Map<String, String> getUrls() {
        return urls;
    }

    public Map<String, String> getExtras() {
        return extras;
    }

    public JsonObject getMetadata() {
        return metadata;
    }

    public JsonObject getProject() {
        return project;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public boolean isVerifyingInternalEanChecksum() {
        return verifyInternalEanChecksum;
    }
}

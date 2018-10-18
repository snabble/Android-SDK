package io.snabble.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Utils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Receipts {
    private static final String SHARED_PREFERENCES_TAG = "snabble_receipts";

    private interface ReceiptDownloadCallback {
        void success(ReceiptInfo receiptInfo);
        void failure();
    }

    private Map<String, ReceiptInfo> receiptInfoList = new HashMap<>();
    private Project project;
    private OkHttpClient okHttpClient;
    private File storageDirectory;
    private SharedPreferences sharedPreferences;

    public Receipts(Project project) {
        this.project = project;
        this.okHttpClient = project.getOkHttpClient();

        storageDirectory = new File(Snabble.getInstance().getInternalStorageDirectory(), "receipts/");
        storageDirectory.mkdirs();

        Context context = Snabble.getInstance().getApplication();
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);

        loadFromSharedPreferences();
    }

    public ReceiptInfo[] getReceiptInfos() {
        Collection<ReceiptInfo> receiptInfos = receiptInfoList.values();
        return receiptInfos.toArray(new ReceiptInfo[receiptInfos.size()]);
    }

    public void download(final ReceiptInfo receiptInfo, final ReceiptDownloadCallback callback) {
        if (receiptInfo.isDownloaded()) {
            callback.success(receiptInfo);
            return;
        }

        download(receiptInfo.getUrl(), callback);
    }

    public void download(final String url, final ReceiptDownloadCallback callback) {
        if (url == null) {
            callback.failure();
            return;
        }

        final String absoluteUrl = Snabble.getInstance().absoluteUrl(url);
        final String id = Utils.sha1Hex(absoluteUrl);
        if (!receiptInfoList.containsKey(id)) {
            final ReceiptInfo receiptInfo = new ReceiptInfo(id, absoluteUrl);
            receiptInfoList.put(id, receiptInfo);

            final Request request = new Request.Builder()
                    .url(absoluteUrl)
                    .get()
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        ResponseBody body = response.body();
                        if (body == null) {
                            callback.failure();
                            return;
                        }

                        File file = new File(storageDirectory, receiptInfo.getId());
                        FileOutputStream fos = new FileOutputStream(file);
                        IOUtils.copy(body.byteStream(), fos);
                        receiptInfo.setFilePath(file.getAbsolutePath());
                        saveReceiptInfo(receiptInfo);

                        if (callback != null) {
                            callback.success(receiptInfo);
                        }
                    } else {
                        callback.failure();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    callback.failure();
                }
            });
        }
    }

    private void loadFromSharedPreferences() {
        Map<String, ?> map = sharedPreferences.getAll();
        for(Map.Entry<String, ?> entry : map.entrySet()) {
            String json = (String)entry.getValue();
            receiptInfoList.put(entry.getKey(), GsonHolder.get().fromJson(json, ReceiptInfo.class));
        }
    }

    private void saveReceiptInfo(ReceiptInfo receiptInfo) {
        sharedPreferences.edit().putString(receiptInfo.getId(), GsonHolder.get().toJson(receiptInfo)).apply();
    }
}

package io.snabble.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Receipts {
    private static final String SHARED_PREFERENCES_TAG = "snabble_receipts";

    public interface ReceiptDownloadCallback {
        void success(ReceiptInfo receiptInfo);
        void failure();
    }

    private Map<String, ReceiptInfo> receiptInfoList = new HashMap<>();
    private File storageDirectory;
    private SharedPreferences sharedPreferences;

    public Receipts() {
        storageDirectory = new File(Snabble.getInstance().getInternalStorageDirectory(), "receipts/");
        storageDirectory.mkdirs();

        Context context = Snabble.getInstance().getApplication();
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);
    }

    public ReceiptInfo[] getReceiptInfos() {
        Collection<ReceiptInfo> receiptInfos = receiptInfoList.values();
        ReceiptInfo[] arr = receiptInfos.toArray(new ReceiptInfo[receiptInfos.size()]);

        Arrays.sort(arr, new Comparator<ReceiptInfo>() {
            @Override
            public int compare(ReceiptInfo l, ReceiptInfo r) {
                long t1 = l.getTimestamp();
                long t2 = r.getTimestamp();

                if (t1 > t2) {
                    return -1;
                } else if (t1 < t2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        return arr;
    }

    public void removeAll() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (ReceiptInfo receiptInfo : receiptInfoList.values()) {
            editor.remove(receiptInfo.getId());
            FileUtils.deleteQuietly(new File(receiptInfo.getFilePath()));
        }

        editor.apply();
        receiptInfoList.clear();
    }

    public void remove(final ReceiptInfo receiptInfo) {
        sharedPreferences.edit().remove(receiptInfo.getId()).apply();
        receiptInfoList.remove(receiptInfo.getId());
        FileUtils.deleteQuietly(new File(receiptInfo.getFilePath()));
    }

    public void cancelDownload(final ReceiptInfo receiptInfo) {
        if(receiptInfo.call != null) {
            receiptInfo.call.cancel();
            receiptInfo.call = null;
        }
    }

    void retrieve(final Project project,
                          final CheckoutApi.CheckoutProcessResponse checkoutProcessResponse,
                          final String shopName,
                          final String price) {
        ReceiptPoller receiptPoller = new ReceiptPoller(project,
                checkoutProcessResponse, shopName, price, 10);
        receiptPoller.poll();
    }

    ReceiptInfo add(final Project project,
                    final String url,
                    final String shopName,
                    final String price) {
        final String absoluteUrl = Snabble.getInstance().absoluteUrl(url);
        final String id = Utils.sha1Hex(absoluteUrl);

        ReceiptInfo receiptInfo = new ReceiptInfo(id, project.getId(), absoluteUrl, shopName, price);
        receiptInfo.setProject(project);
        saveReceiptInfo(receiptInfo);

        return receiptInfo;
    }

    /**
     * Downloads a receipts pdf and stores it in the projects internal storage directory.
     */
    public void download(final ReceiptInfo receiptInfo,
                         final ReceiptDownloadCallback callback) {
        if (receiptInfo.isDownloaded()) {
            callback.success(receiptInfo);
            return;
        }

        if (receiptInfo.getUrl() == null || receiptInfo.getProject() == null) {
            callback.failure();
            return;
        }

        final Request request = new Request.Builder()
                .url(receiptInfo.getUrl())
                .get()
                .build();

        Call call = receiptInfo.getProject().getOkHttpClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    receiptInfo.call = null;

                    ResponseBody body = response.body();
                    if (body == null) {
                        if (callback != null) {
                            callback.failure();
                        }
                        return;
                    }

                    // .pdf extension is needed for adobe reader to work
                    File file = new File(storageDirectory, receiptInfo.getId() + ".pdf");
                    FileOutputStream fos = new FileOutputStream(file);
                    IOUtils.copy(body.byteStream(), fos);
                    receiptInfo.setFilePath(file.getAbsolutePath());
                    saveReceiptInfo(receiptInfo);

                    if (callback != null) {
                        callback.success(receiptInfo);
                    }
                } else {
                    if (callback != null) {
                        callback.failure();
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    callback.failure();
                }
            }
        });

        receiptInfo.call = call;
    }

    void loadFromSharedPreferences() {
        Map<String, ?> map = sharedPreferences.getAll();
        for(Map.Entry<String, ?> entry : map.entrySet()) {
            String json = (String)entry.getValue();
            ReceiptInfo receiptInfo = GsonHolder.get().fromJson(json, ReceiptInfo.class);

            List<Project> projectList = Snabble.getInstance().getProjects();
            for (Project project : projectList) {
                if (project.getId().equals(receiptInfo.getProjectId())) {
                    receiptInfo.setProject(project);
                    break;
                }
            }

            receiptInfoList.put(entry.getKey(), receiptInfo);
        }
    }

    private void saveReceiptInfo(ReceiptInfo receiptInfo) {
        receiptInfoList.put(receiptInfo.getId(), receiptInfo);
        sharedPreferences.edit().putString(receiptInfo.getId(), GsonHolder.get().toJson(receiptInfo)).apply();
    }

    private class ReceiptPoller {
        private String shopName;
        private String price;
        private Project project;
        private CheckoutApi.CheckoutProcessResponse checkoutProcessResponse;
        private CheckoutApi checkoutApi;
        private Handler handler;
        private int currentTry;
        private int maxTries;

        public ReceiptPoller(Project project,
                             CheckoutApi.CheckoutProcessResponse checkoutProcessResponse,
                             String shopName,
                             String price,
                             int maxTries) {
            this.project = project;
            this.checkoutApi = new CheckoutApi(project);
            this.checkoutProcessResponse = checkoutProcessResponse;
            this.shopName = shopName;
            this.price = price;
            this.handler = new Handler(Looper.getMainLooper());
            this.maxTries = maxTries;
        }

        public void poll() {
            if(currentTry < maxTries) {
                currentTry++;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                }, 1000 * currentTry);
            }
        }

        private void update() {
            checkoutApi.updatePaymentProcess(checkoutProcessResponse, new CheckoutApi.PaymentProcessResult() {
                @Override
                public void success(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {
                    String receiptLink = checkoutProcessResponse.getReceiptLink();
                    if (receiptLink != null) {
                        ReceiptInfo receiptInfo = add(project, receiptLink, shopName, price);
                        if (Snabble.getInstance().getConfig().enableReceiptAutoDownload) {
                            download(receiptInfo, null);
                        }
                    } else {
                        poll();
                    }
                }

                @Override
                public void aborted() {
                    poll();
                }

                @Override
                public void error() {
                    poll();
                }
            });
        }
    }
}

package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class Receipts {

    public interface ReceiptInfoCallback {
        void success(ReceiptInfo[] receiptInfos);
        void failure();
    }

    public interface ReceiptDownloadCallback {
        void success(File pdf);
        void failure();
    }

    private ReceiptsApi receiptsApi;
    private Call call;

    public Receipts() {
        receiptsApi = new ReceiptsApi();
    }

    public void getReceiptInfos(final ReceiptInfoCallback receiptInfoCallback) {
        receiptsApi.get(new ReceiptsApi.ReceiptUpdateCallback() {
            @Override
            public void success(final ReceiptInfo[] receiptInfos) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        receiptInfoCallback.success(receiptInfos);
                    }
                });
            }

            @Override
            public void failure() {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        receiptInfoCallback.failure();
                    }
                });
            }
        });
    }

    public void cancelDownload() {
        if (call != null) {
            call.cancel();
            call = null;
        }
    }

    /**
     * Downloads a receipts pdf and stores it in the projects internal storage directory.
     */
    public void download(final ReceiptInfo receiptInfo,
                         final ReceiptDownloadCallback callback) {
        if (receiptInfo.getPdfUrl() == null) {
            callback.failure();
            return;
        }

        final Request request = new Request.Builder()
                .url(receiptInfo.getPdfUrl())
                .get()
                .build();

        cancelDownload();

        // .pdf extension is needed for adobe reader to work
        final File file = new File(Snabble.getInstance().getApplication().getCacheDir(),
                receiptInfo.getId() + ".pdf");

        if (file.exists()) {
            if (callback != null) {
                callback.success(file);
            }

            return;
        }

        Project project = null;

        Snabble snabble = Snabble.getInstance();
        for (Project p : snabble.getProjects()) {
            if (p.getId().equals(receiptInfo.getProjectId())) {
                project = p;
            }
            break;
        }

        if (project == null) {
            callback.failure();
            return;
        }

        call = project.getOkHttpClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    FileOutputStream fos = new FileOutputStream(file);
                    IOUtils.copy(response.body().byteStream(), fos);

                    if (callback != null) {
                        callback.success(file);
                    }
                } else {
                    if (callback != null) {
                        callback.failure();
                    }
                }

                response.close();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    callback.failure();
                }
            }
        });
    }
}

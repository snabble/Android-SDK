package io.snabble.sdk;

import java.io.IOException;

import io.snabble.sdk.utils.Downloader;
import okhttp3.Response;
import okhttp3.ResponseBody;

class ProductDatabaseDownloader extends Downloader {
    private static final String MIMETYPE_DELTA = "application/vnd+sellfio.appdb+sql";
    private static final String MIMETYPE_FULL = "application/vnd+sellfio.appdb+sqlite3";

    private SnabbleSdk sdk;
    private ProductDatabase productDatabase;

    private boolean sameRevision;

    public ProductDatabaseDownloader(SnabbleSdk sdk,
                                     ProductDatabase productDatabase) {
        super(sdk.getOkHttpClient());

        this.sdk = sdk;
        this.productDatabase = productDatabase;
    }

    @Override
    public void onStartDownload() {
        getHeaders().put("Accept", MIMETYPE_DELTA);
        sameRevision = false;

        if (productDatabase.getRevisionId() != -1) {
            String url = sdk.getAppDbUrl()
                    + "?havingRevision=" + productDatabase.getRevisionId()
                    + "&schemaVersion=" + productDatabase.getSchemaVersionMajor()
                    + "." + productDatabase.getSchemaVersionMinor();
            setUrl(url);
        } else {
            setUrl(sdk.getAppDbUrl());
        }
    }

    @Override
    protected void onDownloadFailed(Response response) {
        if (response != null && response.code() == 304) {
            sameRevision = true;
            productDatabase.updateLastUpdateTimestamp(System.currentTimeMillis());
        } else {
            sameRevision = false;
        }
    }

    public boolean wasSameRevision() {
        return sameRevision;
    }

    @Override
    protected void onResponse(Response response) throws IOException {
        String contentType = response.headers().get("Content-Type");
        sameRevision = false;

        if (contentType != null) {
            ResponseBody body = response.body();
            if (body != null) {
                switch (contentType) {
                    case MIMETYPE_DELTA:
                        productDatabase.applyDeltaUpdate(body.byteStream());
                        break;
                    case MIMETYPE_FULL:
                        productDatabase.applyFullUpdate(body.byteStream());
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
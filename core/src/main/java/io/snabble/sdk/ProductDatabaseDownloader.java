package io.snabble.sdk;

import android.content.res.Resources;

import java.io.IOException;

import io.snabble.sdk.utils.Downloader;
import okhttp3.Response;
import okhttp3.ResponseBody;

class ProductDatabaseDownloader extends Downloader {
    private static final String MIMETYPE_DELTA = "application/vnd+snabble.appdb+sql";
    private static final String MIMETYPE_FULL = "application/vnd+snabble.appdb+sqlite3";

    private Project project;
    private ProductDatabase productDatabase;

    private boolean sameRevision;
    private boolean deltaUpdateOnly;

    public ProductDatabaseDownloader(Project project,
                                     ProductDatabase productDatabase) {
        super(project.getOkHttpClient());

        this.project = project;
        this.productDatabase = productDatabase;
    }

    public void update(Callback callback, boolean deltaUpdateOnly) {
        this.deltaUpdateOnly = deltaUpdateOnly;
        loadAsync(callback);
    }

    @Override
    public void onStartDownload() {
        getHeaders().put("Accept", MIMETYPE_DELTA);
        sameRevision = false;

        if (productDatabase.getRevisionId() != -1) {
            String url = project.getAppDbUrl()
                    + "?havingRevision=" + productDatabase.getRevisionId()
                    + "&schemaVersion=" + productDatabase.getSchemaVersionMajor()
                    + "." + productDatabase.getSchemaVersionMinor();
            setUrl(url);
        } else {
            setUrl(project.getAppDbUrl());
        }
    }

    @Override
    protected void onDownloadFailed(Response response) {
        sameRevision = response != null && response.code() == 304;
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
            switch (contentType) {
                case MIMETYPE_DELTA:
                    productDatabase.applyDeltaUpdate(body.byteStream());
                    break;
                case MIMETYPE_FULL:
                    if (deltaUpdateOnly) {
                        response.close();
                        throw new IOException();
                    }

                    productDatabase.applyFullUpdate(body.byteStream());
                    break;
                default:
                    break;
            }
        }

        response.close();
    }
}
package io.snabble.sdk;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.snabble.sdk.auth.Base32String;
import io.snabble.sdk.utils.Downloader;
import io.snabble.sdk.utils.Logger;
import okhttp3.Response;
import okhttp3.ResponseBody;

class ProductDatabaseDownloader extends Downloader {
    private static final String MIMETYPE_DELTA = "application/vnd+snabble.appdb+sql";
    private static final String MIMETYPE_FULL = "application/vnd+snabble.appdb+sqlite3";


    private Project project;
    private ProductDatabase productDatabase;

    private boolean sameRevision;
    private boolean deltaUpdateOnly;

    private File downloadDir;

    public ProductDatabaseDownloader(Project project,
                                     ProductDatabase productDatabase) {
        super(project.getOkHttpClient());

        this.project = project;
        this.productDatabase = productDatabase;

        downloadDir = new File(project.getInternalStorageDirectory(), "/db_downloads/");
    }

    private String getETag() {
        File[] files = downloadDir.listFiles();

        if (files == null) {
            return null;
        }

        File newestFile = null;
        for (File file : files) {
            if (newestFile == null) {
                newestFile = file;
            } else {
                if (FileUtils.isFileNewer(file, newestFile)) {
                    newestFile = file;
                }
            }
        }

        if (newestFile != null) {
            try {
                return new String(Base32String.decode(newestFile.getName()));
            } catch (Base32String.DecodingException e) {
                return null;
            }
        }

        return null;
    }

    private File getTempFile(String eTag) {
        if (eTag == null) {
            return null;
        }

        String fileName = Base32String.encode(eTag.getBytes());
        File[] files = downloadDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(fileName)) {
                    return file;
                } else {
                    FileUtils.deleteQuietly(file);
                }
            }
        }

        return new File(downloadDir, fileName);
    }

    public void update(Callback callback, boolean deltaUpdateOnly) {
        this.deltaUpdateOnly = deltaUpdateOnly;
        loadAsync(callback);
    }

    @Override
    public void onStartDownload() {
        getHeaders().clear();
        getHeaders().put("Accept", MIMETYPE_DELTA);

        String eTag = getETag();
        if (eTag != null) {
            File tempFile = getTempFile(eTag);

            // request range if file exists and we have a strong ETag
            if (tempFile != null && tempFile.exists() && !eTag.startsWith("W/")) {
                getHeaders().put("If-Range", eTag);
                getHeaders().put("Range", "bytes=" + FileUtils.sizeOf(tempFile) + "-");
            }
        }

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
        String contentType = response.header("Content-Type");
        String eTag = response.header("ETag");

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

                    if (response.code() == 206) {
                        Logger.d("Resuming product database download: %s, %s", eTag, response.header("Content-Range"));
                    } else {
                        Logger.d("Starting new product database download: %s, 0/%s", eTag, response.header("Content-Length", "unknown"));
                    }

                    if (body != null) {
                        File tempFile = getTempFile(eTag);
                        if (tempFile != null) {
                            if (response.code() == 416){
                                // range invalid
                                FileUtils.deleteQuietly(tempFile);
                                throw new IOException("Invalid range");
                            } else {
                                // continue with range
                                boolean rangeSupported = response.code() == 206;
                                FileOutputStream fos = FileUtils.openOutputStream(tempFile, rangeSupported);
                                try {
                                    IOUtils.copy(body.byteStream(), fos);
                                    fos.close();
                                    productDatabase.applyFullUpdate(FileUtils.openInputStream(tempFile));
                                    FileUtils.deleteQuietly(tempFile);
                                } catch (IOException e) {
                                    fos.close();
                                    throw e;
                                }
                            }
                        } else {
                            productDatabase.applyFullUpdate(body.byteStream());
                        }

                        Logger.d("Finished product database download: %s");
                        break;
                    }
            }

            response.close();
        }
    }
}
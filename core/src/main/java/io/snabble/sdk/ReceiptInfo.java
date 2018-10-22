package io.snabble.sdk;

import okhttp3.Call;

public class ReceiptInfo {
    private String id;
    private long timestamp;
    private String url;
    private String filePath;
    private String shopName;
    private String price;
    private String projectId;
    private transient Project project;
    transient Call call;

    public ReceiptInfo(String id, String projectId, String url, String shopName, String price) {
        this.id = id;
        this.projectId = projectId;
        this.url = url;
        this.shopName = shopName;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUrl() {
        return url;
    }

    public String getFilePath() {
        return filePath;
    }

    void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isDownloaded() {
        return filePath != null;
    }

    public String getShopName() {
        return shopName;
    }

    public String getPrice() {
        return price;
    }

    void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public String getProjectId() {
        return projectId;
    }
}


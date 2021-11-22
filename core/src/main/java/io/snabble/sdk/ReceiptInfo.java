package io.snabble.sdk;

import java.util.Date;

public class ReceiptInfo {
    private final String id;
    private final String projectId;
    private final long timestamp;
    private final String pdfUrl;
    private final String shopName;
    private final String price;

    public ReceiptInfo(String id, String projectId, long timestamp, String pdfUrl, String shopName, String price) {
        this.id = id;
        this.projectId = projectId;
        this.timestamp = timestamp;
        this.pdfUrl = pdfUrl;
        this.shopName = shopName;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    @Deprecated
    public Date getDate() {
        return new Date(timestamp);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getShopName() {
        return shopName;
    }

    public String getPrice() {
        return price;
    }
}
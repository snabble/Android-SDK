package io.snabble.sdk;

import java.util.Date;

public class ReceiptInfo {
    private String id;
    private String projectId;
    private Date date;
    private String pdfUrl;
    private String shopName;
    private String price;

    public ReceiptInfo(String id, String projectId, Date date, String pdfUrl, String shopName, String price) {
        this.id = id;
        this.projectId = projectId;
        this.date = date;
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

    public Date getDate() {
        return date;
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


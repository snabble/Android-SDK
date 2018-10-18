package io.snabble.sdk;
public class ReceiptInfo {
    private String id;
    private long timestamp;
    private String url;
    private String filePath;
    private String shopName;
    private String price;

    public ReceiptInfo(String id, String url, String shopName, String price) {
        this.id = id;
        this.url = url;
        this.shopName = shopName;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isDownloaded() {
        return getFilePath() != null;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}


package io.snabble.sdk.ui.scanner;

public class Barcode {
    private BarcodeFormat format;
    private String text;
    private long timestamp;

    Barcode(BarcodeFormat format, String text, long timestamp) {
        this.format = format;
        this.text = text;
        this.timestamp = timestamp;
    }

    public BarcodeFormat getFormat() {
        return format;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Barcode{" +
                "format=" + format +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

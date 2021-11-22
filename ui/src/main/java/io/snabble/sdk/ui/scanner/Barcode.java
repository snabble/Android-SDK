package io.snabble.sdk.ui.scanner;

import androidx.annotation.NonNull;

import io.snabble.sdk.BarcodeFormat;

public class Barcode {
    private final BarcodeFormat format;
    private final String text;
    private final long timestamp;

    public Barcode(BarcodeFormat format, String text, long timestamp) {
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

    @NonNull
    @Override
    public String toString() {
        return "Barcode{" +
                "format=" + format +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
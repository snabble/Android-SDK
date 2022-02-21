package io.snabble.sdk.ui.scanner;

import io.snabble.sdk.utils.Logger;

public class FalsePositiveFilter {
    private final int repeatsNeeded;
    private Barcode lastBarcode;
    private int repeats = 0;

    public FalsePositiveFilter() {
        this.repeatsNeeded = 2;
    }

    public FalsePositiveFilter(int repeatsNeeded) {
        this.repeatsNeeded = repeatsNeeded;
    }

    public synchronized void reset() {
        repeats = 0;
        lastBarcode = null;
    }

    public synchronized Barcode filter(Barcode barcode) {
        if (lastBarcode == null
        || (lastBarcode.getText().equals(barcode.getText())
         && lastBarcode.getFormat().equals(barcode.getFormat()))) {
            repeats++;
            lastBarcode = barcode;
        } else {
            Logger.d("Filtered false positive: %s OR %s", lastBarcode, barcode);
            reset();
        }

        if (repeats >= repeatsNeeded) {
            return barcode;
        }

        return null;
    }
}
package io.snabble.sdk.ui.scanner;

import io.snabble.sdk.utils.Logger;

public class FalsePositiveFilter {
    private final static int REPEATS_NEEDED = 2;

    private Barcode lastBarcode;
    private int repeats = 0;

    public FalsePositiveFilter() {

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

        if (repeats >= REPEATS_NEEDED) {
            return barcode;
        }

        return null;
    }
}

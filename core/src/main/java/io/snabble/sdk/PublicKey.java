package io.snabble.sdk;

import io.snabble.sdk.utils.Utils;

public class PublicKey {
    private String cipher;
    private String signature;

    public PublicKey(String cipher) {
        this.cipher = cipher;
        this.signature = Utils.sha256Hex(cipher);
    }

    public String getCipher() {
        return cipher;
    }

    public String getSignature() {
        return signature;
    }
}

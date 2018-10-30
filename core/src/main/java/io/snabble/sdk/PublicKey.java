package io.snabble.sdk;

public class PublicKey {
    private String cipher;

    public PublicKey(String cipher) {
        this.cipher = cipher;
    }

    public String getCipher() {
        return cipher;
    }
}

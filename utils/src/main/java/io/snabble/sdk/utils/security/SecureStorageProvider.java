package io.snabble.sdk.utils.security;

public interface SecureStorageProvider {
    void invalidate();
    byte[] encrypt(byte[] data);
    byte[] decrypt(byte[] encrypted);
}

package io.snabble.sdk.auth;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Class to generate RFC 6238 one time passwords.
 *
 * Based on the Google Authenticator implementation.
 *
 * See https://tools.ietf.org/html/rfc6238
 * See https://github.com/google/google-authenticator-android
 */
public class Totp {
    private static final int[] DIGITS_POWER
            // 0  1   2    3     4      5       6        7         8          9
            = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    private int length;
    private int timeStep;
    private Mac mac;

    public Totp(String algorithm, byte[] secret, int length, int timeStep) {
        try {
            mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret, "RAW"));

            this.length = length;
            this.timeStep = timeStep;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public String generate(long seconds) {
        long counter = seconds / timeStep;

        byte[] value = ByteBuffer.allocate(8).putLong(counter).array();
        byte[] hash = mac.doFinal(value);

        // Dynamically truncate the hash
        // OffsetBits are the low order bits of the last byte of the hash
        int offset = hash[hash.length - 1] & 0xF;

        // Grab a positive integer value starting at the given offset.
        int truncatedHash = hashToInt(hash, offset) & 0x7FFFFFFF;
        int pinValue = truncatedHash % DIGITS_POWER[length];

        // Add padding
        String result = Integer.toString(pinValue);
        for (int i = result.length(); i < length; i++) {
            result = "0" + result;
        }
        return result;
    }

    private int hashToInt(byte[] bytes, int start) {
        DataInput input = new DataInputStream(
                new ByteArrayInputStream(bytes, start, bytes.length - start));
        int val;
        try {
            val = input.readInt();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return val;
    }
}

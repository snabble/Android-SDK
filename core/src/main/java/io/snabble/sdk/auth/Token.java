package io.snabble.sdk.auth;

import com.google.gson.annotations.SerializedName;

public class Token {
    @SerializedName("id") public final String id;
    @SerializedName("token") public final String token;
    @SerializedName("issuedAt") public final long issuedAt;
    @SerializedName("expiresAt") public final long expiresAt;

    public Token(String id, String token, long issuedAt, long expiresAt) {
        this.id = id;
        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}

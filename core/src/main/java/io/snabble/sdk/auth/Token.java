package io.snabble.sdk.auth;

public class Token {
    public final String id;
    public final String token;
    public final long issuedAt;
    public final long expiresAt;

    public Token(String id, String token, long issuedAt, long expiresAt) {
        this.id = id;
        this.token = token;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}

package io.snabble.sdk.auth;

import java.util.Objects;

public class AppUser {
    public final String id;
    public final String secret;

    public AppUser(String id, String secret) {
        this.id = id;
        this.secret = secret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppUser appUser = (AppUser) o;
        return Objects.equals(id, appUser.id) &&
                Objects.equals(secret, appUser.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, secret);
    }
}

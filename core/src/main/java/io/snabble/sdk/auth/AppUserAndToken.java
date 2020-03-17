package io.snabble.sdk.auth;

public class AppUserAndToken {
    public final Token token;
    public final AppUser appUser;

    public AppUserAndToken(Token token, AppUser appUser) {
        this.token = token;
        this.appUser = appUser;
    }
}

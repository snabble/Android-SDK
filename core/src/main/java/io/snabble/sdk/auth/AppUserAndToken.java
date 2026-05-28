package io.snabble.sdk.auth;

import com.google.gson.annotations.SerializedName;

public class AppUserAndToken {
    @SerializedName("token") public final Token token;
    @SerializedName("appUser") public final AppUser appUser;

    public AppUserAndToken(Token token, AppUser appUser) {
        this.token = token;
        this.appUser = appUser;
    }
}

package io.snabble.sdk.auth;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.UserPreferences;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenRegistry {
    private Totp totp;
    private final Map<String, Token> tokens = new HashMap<>();
    private UserPreferences userPreferences;
    private String appId;
    private OkHttpClient okHttpClient;
    private long timeOffset;

    public TokenRegistry(OkHttpClient okHttpClient,
                         UserPreferences userPreferences,
                         String appId,
                         String secret) {
        try {
            byte[] secretData = Base32String.decode(secret);

            this.totp = new Totp("HmacSHA256", secretData, 8, 30);
            this.userPreferences = userPreferences;
            this.appId = appId;
            this.okHttpClient = okHttpClient;

            userPreferences.addOnNewAppUserListener(appUser -> invalidate());
        } catch (Base32String.DecodingException e) {
            e.printStackTrace();
        }
    }

    public void invalidate() {
        tokens.clear();
    }

    @Nullable
    private synchronized Token refreshToken(Project project, boolean isRetry) {
        if (totp == null) {
            return null;
        }

        long time = getOffsetTime();

        Logger.d("Getting token for %s, t=%s, c=%s", project.getId(),
                String.valueOf(time),
                String.valueOf(time / 30));

        final AppUser appUser = userPreferences.getAppUser();

        String auth;
        String url;
        if (appUser != null) {
            auth = appId + ":" + totp.generate(time) + ":" + appUser.id + ":" + appUser.secret;
            url = project.getTokensUrl();
        } else {
            auth = appId + ":" + totp.generate(time);
            url = project.getAppUserUrl();
        }

        String base64;

        try {
            base64 = Base64.encodeToString(auth.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            // cant recover from this
            return null;
        }

        Request.Builder request;

        try {
            request = new Request.Builder()
                    .url(url);
        } catch (IllegalArgumentException e) {
            return null;
        }

        if (appUser != null) {
            request = request.get();
        } else {
            request = request.post(RequestBody.create(null, ""));
        }

        request.addHeader("Authorization", "Basic " + base64)
        .addHeader("Client-ID", userPreferences.getClientId())
        .build();

        Response response = null;
        try {
            response = okHttpClient.newCall(request.build()).execute();
            String body = response.body().string();
            response.close();

            if (response.isSuccessful()) {
                Logger.d("Successfully generated token for %s", project.getId());

                adjustTimeOffset(response);
                if (appUser == null) {
                    AppUserAndToken appUserAndToken = GsonHolder.get().fromJson(body, AppUserAndToken.class);
                    userPreferences.setAppUser(appUserAndToken.appUser);
                    tokens.put(project.getId(), appUserAndToken.token);
                    return appUserAndToken.token;
                } else {
                    Token token = GsonHolder.get().fromJson(body, Token.class);
                    tokens.put(project.getId(), token);
                    return token;
                }
            } else {
                if (!isRetry) {
                    Logger.d("Could not generate token, trying again with server time");

                    adjustTimeOffset(response);
                    return refreshToken(project, true);
                } else {
                    Logger.e("Could not generate token: %s", body);
                }
            }
        } catch (IOException e) {
            if (response != null) {
                response.close();
            }
            Logger.e("Could not generate token: %s", e.toString());
        }

        return null;
    }

    private void adjustTimeOffset(Response response) {
        Date serverDate = response.headers().getDate("Date");
        if (serverDate != null) {
            timeOffset = serverDate.getTime() - System.currentTimeMillis();
            Logger.d("timeOffset = %d", timeOffset);
        }
    }

    private long getOffsetTime() {
        return (System.currentTimeMillis() + timeOffset) / 1000;
    }

    /**
     * Synchronously retrieves a token for the project.
     * <p>
     * May do synchronous http requests, if the token is invalid.
     * If a valid token is available, it will be returned without doing http requests.
     * <p>
     * Returns null if not valid token could be generated. (invalid secret, timeouts, no connection)
     */
    @Nullable
    public synchronized Token getToken(@Nullable Project project) {
        if (project == null) {
            return null;
        }

        Token token = tokens.get(project.getId());

        if (token != null) {
            long tokenInterval = (token.expiresAt - token.issuedAt);
            long invalidAt = token.issuedAt + tokenInterval / 2;

            long seconds = getOffsetTime();
            if (seconds >= invalidAt) {
                Logger.d("Token timed out, requesting new token");
                Token newToken = refreshToken(project, false);
                if (newToken != null) {
                    token = newToken;
                }
                Snabble.getInstance().getUsers().update();
            }
        } else {
            token = refreshToken(project, false);
            Snabble.getInstance().getUsers().update();
        }

        return token;
    }

    /**
     * Returns the locally stored token or null if invalid
     */
    @Nullable
    public Token getLocalToken(@NonNull final Project project) {
        final Token token = tokens.get(project.getId());

        if (isValid(token)) {
            return token;
        } else {
            return null;
        }
    }

    @NonNull
    private Boolean isValid(@Nullable final Token token) {
        if (token == null) return false;

        final long tokenIntervalInSeconds = (token.expiresAt - token.issuedAt);
        final long invalidAtInSeconds = token.issuedAt + tokenIntervalInSeconds / 2;

        final long offsetTimeInSeconds = getOffsetTime();
        return offsetTimeInSeconds >= invalidAtInSeconds;
    }
}

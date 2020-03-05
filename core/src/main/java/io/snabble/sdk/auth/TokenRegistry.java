package io.snabble.sdk.auth;

import android.util.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.snabble.sdk.Project;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TokenRegistry {
    private Totp totp;
    private Map<String, Token> tokens = new HashMap<>();
    private String clientId;
    private String appId;
    private OkHttpClient okHttpClient;
    private long timeOffset;

    public TokenRegistry(OkHttpClient okHttpClient, String clientId, String appId, String secret) {
        try {
            byte[] secretData = Base32String.decode(secret);

            this.totp = new Totp("HmacSHA256", secretData, 8, 30);
            this.clientId = clientId;
            this.appId = appId;
            this.okHttpClient = okHttpClient;
        } catch (Base32String.DecodingException e) {
            e.printStackTrace();
        }
    }

    private synchronized Token refreshToken(Project project, boolean isRetry) {
        if (totp == null) {
            return null;
        }

        long time = getOffsetTime();

        Logger.d("Getting token for %s, t=%s, c=%s", project.getId(),
                String.valueOf(time),
                String.valueOf(time / 30));

        String auth = appId + ":" + totp.generate(time);
        String base64;

        try {
            base64 = Base64.encodeToString(auth.getBytes("UTF-8"), Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            // cant recover from this
            return null;
        }

        String url = project.getTokensUrl() + "?role=retailerApp";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Basic " + base64)
                .addHeader("Client-ID", clientId)
                .build();

        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            String body = response.body().string();
            response.close();

            if (response.isSuccessful()) {
                Logger.d("Successfully generated token for %s", project.getId());

                adjustTimeOffset(response);
                Token token = GsonHolder.get().fromJson(body, Token.class);
                tokens.put(project.getId(), token);
                return token;
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
    public synchronized Token getToken(Project project) {
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
            }
        } else {
            token = refreshToken(project, false);
        }

        return token;
    }
}

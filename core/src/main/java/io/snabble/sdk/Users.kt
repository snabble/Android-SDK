package io.snabble.sdk;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.snabble.sdk.auth.AppUser;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class Users {
    public interface UpdateUserCallback {
        void success();
        void failure();
    }

    public interface GetUserCallback {
        void success(Response response);
        void failure();
    }

    private static class Request {
        public String id;
        public String dayOfBirth;
    }

    public static class Response {
        public String id;
        public String dayOfBirth;
        public String bornBeforeOrOn;
    }

    private static class UpdateConsentRequest {
        public String version;
    }

    private Call updateBirthdayCall;
    private Call postConsentCall;
    private final SimpleDateFormat simpleDateFormat;
    private final UserPreferences userPreferences;

    public Users(UserPreferences userPreferences) {
        simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        this.userPreferences = userPreferences;
    }

    public void get(final GetUserCallback callback) {
        final Snabble snabble = Snabble.getInstance();
        String url = snabble.getUsersUrl();

        AppUser appUser = snabble.getUserPreferences().getAppUser();

        if (appUser != null && url != null) {
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .get()
                    .url(url.replace("{appUserID}", appUser.id))
                    .build();

            OkHttpClient okHttpClient = snabble.getProjects().get(0).getOkHttpClient();
            okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<Response>(Response.class) {
                @Override
                public void success(Response response) {
                    callback.success(response);
                }

                @Override
                public void error(Throwable t) {
                    callback.failure();
                }
            });
        } else {
            callback.failure();
        }
    }

    public void setBirthday(Date birthday, final UpdateUserCallback updateUserCallback) {
        final Snabble snabble = Snabble.getInstance();
        String url = snabble.getUsersUrl();

        AppUser appUser = snabble.getUserPreferences().getAppUser();

        if (appUser != null && url != null) {
            Request updateBirthdayRequest = new Request();
            updateBirthdayRequest.id = appUser.id;
            updateBirthdayRequest.dayOfBirth = simpleDateFormat.format(birthday);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                    GsonHolder.get().toJson(updateBirthdayRequest));

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .patch(requestBody)
                    .url(url.replace("{appUserID}", appUser.id))
                    .build();

            OkHttpClient okHttpClient = snabble.getProjects().get(0).getOkHttpClient();
            updateBirthdayCall = okHttpClient.newCall(request);
            updateBirthdayCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    updateBirthdayCall = null;
                    updateUserCallback.failure();
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) {
                    if (response.isSuccessful()) {
                        updateBirthdayCall = null;
                        updateUserCallback.success();
                    } else {
                        updateBirthdayCall = null;
                        updateUserCallback.failure();
                    }
                }
            });
        } else {
            updateUserCallback.failure();
        }
    }

    public void postPendingConsents() {
        UserPreferences.ConsentStatus consentStatus = userPreferences.getConsentStatus();
        if (consentStatus == UserPreferences.ConsentStatus.TRANSMIT_FAILED) {
            postConsentVersion();
        } else if (consentStatus == UserPreferences.ConsentStatus.TRANSMITTING) {
            if (postConsentCall == null) {
                postConsentVersion();
            }
        }
    }

    public void setConsent(String version) {
        userPreferences.setConsentVersion(version);
        postConsentVersion();
    }

    private void postConsentVersion() {
        final Snabble snabble = Snabble.getInstance();
        String url = snabble.getConsentUrl();
        AppUser appUser = userPreferences.getAppUser();

        if (appUser == null || url == null) {
            return;
        }

        String version = userPreferences.getConsentVersion();
        UpdateConsentRequest updateBirthdayRequest = new UpdateConsentRequest();
        updateBirthdayRequest.version = version;
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                GsonHolder.get().toJson(updateBirthdayRequest));

        okhttp3.Request request = new okhttp3.Request.Builder()
                .post(requestBody)
                .url(url.replace("{appUserID}", appUser.id))
                .build();

        userPreferences.setConsentStatus(UserPreferences.ConsentStatus.TRANSMITTING);

        OkHttpClient okHttpClient = snabble.getProjects().get(0).getOkHttpClient();
        postConsentCall = okHttpClient.newCall(request);
        postConsentCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                postConsentCall = null;
                userPreferences.setConsentStatus(UserPreferences.ConsentStatus.TRANSMIT_FAILED);
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) {
                if (response.isSuccessful()) {
                    postConsentCall = null;
                    userPreferences.setConsentStatus(UserPreferences.ConsentStatus.ACCEPTED);
                } else {
                    postConsentCall = null;
                    if (response.code() == 400) {
                        userPreferences.setConsentStatus(UserPreferences.ConsentStatus.ACCEPTED);
                    } else {
                        userPreferences.setConsentStatus(UserPreferences.ConsentStatus.TRANSMIT_FAILED);
                    }
                }
            }
        });
    }

    public void update() {
        get(new GetUserCallback() {
            @Override
            public void success(Response response) {
                Date birthday = null;

                try {
                    if (response.dayOfBirth != null) {
                        birthday = simpleDateFormat.parse(response.dayOfBirth);
                    } else if (response.bornBeforeOrOn != null) {
                        birthday = simpleDateFormat.parse(response.bornBeforeOrOn);
                    }
                } catch (Exception ignored) { }

                Snabble.getInstance().getUserPreferences().setBirthday(birthday);
            }

            @Override
            public void failure() {

            }
        });
    }

    public void cancelUpdatingUser() {
        if (updateBirthdayCall != null) {
            updateBirthdayCall.cancel();
        }
    }

}

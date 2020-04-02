package io.snabble.sdk;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.snabble.sdk.auth.AppUser;
import io.snabble.sdk.utils.GsonHolder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Users {
    private Call updateBirthdayCall;

    public interface UpdateAgeCallback {
        void success();
        void failure();
    }

    private final SimpleDateFormat simpleDateFormat;

    private static class UpdateBirthdayRequest {
        public String dayOfBirth;
        public String id;
    }

    public Users() {
        simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void updateBirthday(Date birthday, final UpdateAgeCallback updateAgeCallback) {
        final Snabble snabble = Snabble.getInstance();
        String url = snabble.getUsersUrl();

        AppUser appUser = snabble.getUserPreferences().getAppUser();

        if (appUser != null && url != null) {
            UpdateBirthdayRequest updateBirthdayRequest = new UpdateBirthdayRequest();
            updateBirthdayRequest.id = appUser.id;
            updateBirthdayRequest.dayOfBirth = simpleDateFormat.format(birthday);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                    GsonHolder.get().toJson(updateBirthdayRequest));

            Request request = new Request.Builder()
                    .patch(requestBody)
                    .url(url.replace("{appUserID}", appUser.id))
                    .build();

            OkHttpClient okHttpClient = snabble.getProjects().get(0).getOkHttpClient();
            updateBirthdayCall = okHttpClient.newCall(request);
            updateBirthdayCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    updateBirthdayCall = null;
                    updateAgeCallback.failure();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        updateBirthdayCall = null;
                        updateAgeCallback.success();
                    } else {
                        updateBirthdayCall = null;
                        updateAgeCallback.failure();
                    }
                }
            });
        }
    }

    public void cancelUpdatingBirthday() {
        if (updateBirthdayCall != null) {
            updateBirthdayCall.cancel();
        }
    }

}

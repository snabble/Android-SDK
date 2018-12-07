package io.snabble.sdk.utils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public abstract class JsonCallback<T, T2> implements Callback {
    private final Class<T> successClass;
    private final Class<T2> failureClass;

    private int responseCode;

    public JsonCallback(Class<T> successClass, Class<T2> failureClass) {
        this.successClass = successClass;
        this.failureClass = failureClass;
    }

    @Override
    public void onResponse(Call call, Response response) {
        try {
            String body = response.body().string(); // string closes response
            responseCode = response.code();

            if(response.isSuccessful()) {
                T obj = GsonHolder.get().fromJson(body, successClass);
                success(obj);
            } else {
                handleFailure(body);
            }
        } catch (IOException e) {
            response.close();
            error(e);
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {
        error(e);
    }

    public int responseCode() {
        return responseCode;
    }

    protected void handleFailure(String body) {
        try {
            T2 obj = GsonHolder.get().fromJson(body, failureClass);
            failure(obj);
        } catch (Exception e) {
            error(e);
        }
    }

    public abstract void success(T obj);
    public abstract void failure(T2 obj);
    public abstract void error(Throwable t);
}

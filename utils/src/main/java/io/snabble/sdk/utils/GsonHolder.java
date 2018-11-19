package io.snabble.sdk.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonHolder {
    private static Gson INSTANCE = new GsonBuilder().create();

    public static Gson get() {
        return INSTANCE;
    }
}

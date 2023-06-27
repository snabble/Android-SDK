package io.snabble.sdk.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.snabble.sdk.utils.gson.FileExclusionStrategy;

public class GsonHolder {
    private static final Gson INSTANCE = new GsonBuilder()
            .addSerializationExclusionStrategy(new FileExclusionStrategy())
            .create();

    public static Gson get() {
        return INSTANCE;
    }
}

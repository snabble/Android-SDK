package io.snabble.sdk.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtils {
    public static String getStringOpt(JsonObject jsonObject, String key, String defaultValue){
        if(jsonObject != null && jsonObject.has(key)){
            return jsonObject.get(key).getAsString();
        } else {
            return defaultValue;
        }
    }

    public static String[] getStringArrayOpt(JsonObject jsonObject, String key, String[] defaultValue){
        if(jsonObject != null && jsonObject.has(key)){
            JsonElement jsonElement = jsonObject.get(key);
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = jsonObject.get(key).getAsJsonArray();
                String[] strings = new String[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    strings[i] = jsonArray.get(i).getAsString();
                }
                return strings;
            }
        }

        return defaultValue;
    }

    public static double getDoubleOpt(JsonObject jsonObject, String key, double defaultValue){
        if(jsonObject != null && jsonObject.has(key)){
            return jsonObject.get(key).getAsDouble();
        } else {
            return defaultValue;
        }
    }

    public static long getLongOpt(JsonObject jsonObject, String key, long defaultValue){
        if(jsonObject != null && jsonObject.has(key)){
            return jsonObject.get(key).getAsLong();
        } else {
            return defaultValue;
        }
    }

    public static int getIntOpt(JsonObject jsonObject, String key, int defaultValue){
        if(jsonObject != null && jsonObject.has(key)){
            return jsonObject.get(key).getAsInt();
        } else {
            return defaultValue;
        }
    }

    public static boolean getBooleanOpt(JsonObject jsonObject, String key, boolean defaultValue){
        if(jsonObject != null && jsonObject.has(key)){
            return jsonObject.get(key).getAsBoolean();
        } else {
            return defaultValue;
        }
    }
}

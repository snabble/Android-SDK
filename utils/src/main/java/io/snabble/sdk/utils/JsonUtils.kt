@file:JvmName("JsonUtils")
package io.snabble.sdk.utils

import com.google.gson.JsonObject

fun JsonObject.getString(key: String, defaultValue: String) =
    get(key)?.asString ?: defaultValue

fun JsonObject.getStringOpt(key: String, defaultValue: String?) =
    get(key)?.asString ?: defaultValue

fun JsonObject.getStringListOpt(
    key: String,
    defaultValue: List<String?>?
): List<String?>? {
    if (has(key)) {
        get(key)?.let { jsonElement ->
            if (jsonElement.isJsonArray) {
                val jsonArray = jsonElement.asJsonArray
                return jsonArray.map { it.asString }
            }
        }
    }
    return defaultValue
}

fun JsonObject.getDoubleOpt(key: String, defaultValue: Double) =
    get(key)?.asDouble ?: defaultValue

fun JsonObject.getLongOpt(key: String, defaultValue: Long) =
    get(key)?.asLong ?: defaultValue

fun JsonObject.getIntOpt(key: String, defaultValue: Int) =
    get(key)?.asInt ?: defaultValue

fun JsonObject.getBooleanOpt(key: String, defaultValue: Boolean) =
    get(key)?.asBoolean ?: defaultValue
package io.snabble.sdk.utils

import com.google.gson.JsonObject

object JsonUtils {
    @JvmStatic
    fun JsonObject.getString(key: String, defaultValue: String)
            = get(key)?.asString ?: defaultValue

    @JvmStatic
    fun JsonObject.getStringOpt(key: String, defaultValue: String?)
        = get(key)?.asString ?: defaultValue

    @JvmStatic
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

    @JvmStatic
    fun JsonObject.getDoubleOpt(key: String, defaultValue: Double)
        = get(key)?.asDouble ?: defaultValue

    @JvmStatic
    fun JsonObject.getLongOpt(key: String, defaultValue: Long)
        = get(key)?.asLong ?: defaultValue

    @JvmStatic
    fun JsonObject.getIntOpt(key: String, defaultValue: Int)
        = get(key)?.asInt ?: defaultValue

    @JvmStatic
    fun JsonObject.getBooleanOpt(key: String, defaultValue: Boolean)
        = get(key)?.asBoolean ?: defaultValue
}
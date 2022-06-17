@file:JvmName("PropertiesExtensions")
package io.snabble.sdk

import java.util.*

fun Properties.getBoolean(key: String, default: Boolean) =
    getProperty(key).toBooleanStrictOrNull() ?: default
fun Properties.getLong(key: String, default: Long) =
    getProperty(key).toLongOrNull() ?: default
fun Properties.getFloat(key: String, default: Float) =
    getProperty(key).toFloatOrNull() ?: default
@file:JvmName("DatabaseUtils")
package io.snabble.sdk

import androidx.annotation.RestrictTo

@JvmName("bindArgs")
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal fun String.bindSqlArgs(args: Array<String>?): String {
    if (args == null) {
        return this
    }
    var printSql = this
    for (arg in args) {
        printSql = printSql.replaceFirst("\\?".toRegex(), "'$arg'")
    }
    return printSql
}
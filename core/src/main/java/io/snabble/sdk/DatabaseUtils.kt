@file:JvmName("DatabaseUtils")
package io.snabble.sdk

@JvmName("bindArgs")
/**
 * Binds the strings in args to ocurrences of '?' in a sql string
 */
fun String.bindSqlArgs(args: Array<String>?): String {
    if (args == null) {
        return this
    }
    var printSql = this
    for (arg in args) {
        printSql = printSql.replaceFirst("\\?".toRegex(), "'$arg'")
    }
    return printSql
}
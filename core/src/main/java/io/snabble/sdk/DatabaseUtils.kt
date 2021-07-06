@file:JvmName("DatabaseUtils")
package io.snabble.sdk

@JvmName("bindArgs")
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
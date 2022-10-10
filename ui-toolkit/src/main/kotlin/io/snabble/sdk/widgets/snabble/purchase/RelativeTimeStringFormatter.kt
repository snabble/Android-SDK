package io.snabble.sdk.widgets.snabble.purchase

import android.text.format.DateUtils

internal interface RelativeTimeStringFormatter {

    fun format(timeInMillis: Long, nowInMillis: Long): String
}

internal class RelativeTimeStringFormatterImpl : RelativeTimeStringFormatter {

    override fun format(timeInMillis: Long, nowInMillis: Long): String =
        DateUtils
            .getRelativeTimeSpanString(
                timeInMillis,
                nowInMillis,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_NUMERIC_DATE
            )
            .toString()
}

package io.snabble.sdk.ui.utils

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

fun Vibrator.vibrateCompat(timeInMillis: Long) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        vibrate(VibrationEffect.createOneShot(timeInMillis, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION") vibrate(timeInMillis)
    }

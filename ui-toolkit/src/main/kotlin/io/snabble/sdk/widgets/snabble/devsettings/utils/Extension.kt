package io.snabble.sdk.widgets.snabble.devsettings.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun Context.copyToClipBoard(msg: String, id: String) {
    val clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(msg, id))
    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show()
}

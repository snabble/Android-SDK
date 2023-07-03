package io.snabble.sdk.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import io.snabble.sdk.ui.R

open class ProgressDialog(context: Context) : AlertDialog(context) {

    private var indeterminate: Boolean = true
    private var message: CharSequence? = null

    private var messageView: TextView? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val view: View = inflateView(context)
            .also(::bindView)
        setView(view)

        if (message != null) setMessage(message)

        setIndeterminate(indeterminate)

        super.onCreate(savedInstanceState)
    }

    @SuppressLint("InflateParams")
    private fun inflateView(context: Context): View =
        LayoutInflater.from(context).inflate(R.layout.snabble_progress_dialog, null)

    private fun bindView(view: View) {
        progressBar = view.findViewById(android.R.id.progress) as? ProgressBar
        messageView = view.findViewById(android.R.id.message) as? TextView
    }

    private fun setIndeterminate(indeterminate: Boolean) {
        val progressBar = progressBar

        if (progressBar != null) {
            progressBar.isIndeterminate = indeterminate
        } else {
            this.indeterminate = indeterminate
        }
    }

    override fun setMessage(message: CharSequence?) {
        val messageView = messageView
        if (messageView != null) {
            messageView.text = message
        } else {
            this.message = message
        }
    }
}

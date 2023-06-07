package io.snabble.sdk.ui.payment.externalbilling.ui.widgets

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import io.snabble.sdk.ui.R

class SubjectAlertDialog(context: Context) : Dialog(context) {

    private var subjectMessageClickListener: SubjectMessageClickListener? = null
    private var skipClick: SubjectClickListener? = null
    private var onCancelClick: SubjectClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.snabble_subject_alert_dialog)
        window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val input = findViewById<TextInputLayout>(R.id.text_input_subject)
        val add = findViewById<MaterialButton>(R.id.subject_add)
        val skip = findViewById<MaterialButton>(R.id.subject_skip)

        add.setOnClickListener {
            subjectMessageClickListener?.onClick(input.editText?.text.toString())
            dismiss()
        }
        skip.setOnClickListener {
            skipClick?.onClick()
            dismiss()
        }
    }

    override fun dismiss() {
        super.dismiss()
        onCancelClick?.onClick()
    }

    fun setOnCanceledListener(onClick: SubjectClickListener) = apply {
        onCancelClick = onClick
    }

    fun addMessageClickListener(onClick: SubjectMessageClickListener): SubjectAlertDialog = apply {
        subjectMessageClickListener = onClick

    }

    fun addSkipClickListener(click: SubjectClickListener): SubjectAlertDialog = apply {
        skipClick = click
    }
}

fun interface SubjectMessageClickListener {

    fun onClick(message: String)
}

fun interface SubjectClickListener {

    fun onClick()
}

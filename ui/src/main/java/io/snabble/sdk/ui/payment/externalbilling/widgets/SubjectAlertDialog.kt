package io.snabble.sdk.ui.payment.externalbilling.widgets

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import io.snabble.sdk.ui.R

class SubjectAlertDialog(context: Context) : Dialog(context) {

    private var subjectMessageClickListener: SubjectMessageClickListener? = null
    private var skipClick: SubjectClickListener? = null
    private var abortClick: SubjectClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.snabble_subject_alert_dialog)
        window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        val input = findViewById<TextInputLayout>(R.id.text_input_subject)
        val add = findViewById<MaterialButton>(R.id.subject_add)
        val skip = findViewById<MaterialButton>(R.id.subject_skip)
        val abort = findViewById<ImageView>(R.id.subject_abort)

        add.setOnClickListener {
            subjectMessageClickListener?.onClick(input.editText?.text.toString())
            dismiss()
        }
        skip.setOnClickListener {
            skipClick?.onClick()
            dismiss()
        }
        abort.setOnClickListener {
            abortClick?.onClick()
            dismiss()
        }
    }

    fun addClickListener(onClick: SubjectMessageClickListener): SubjectAlertDialog {
        this.subjectMessageClickListener = onClick
        return this
    }

    fun skipClickListener(click: SubjectClickListener): SubjectAlertDialog {
        skipClick = click
        return this
    }

    fun abortClickListener(click: SubjectClickListener): SubjectAlertDialog {
        abortClick = click
        return this
    }
}

interface SubjectMessageClickListener {

    fun onClick(message: String)
}

interface SubjectClickListener {

    fun onClick()
}

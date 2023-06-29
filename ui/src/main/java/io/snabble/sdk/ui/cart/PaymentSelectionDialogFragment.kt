package io.snabble.sdk.ui.cart

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.snabble.accessibility.accessibility
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.PaymentInputViewHelper.openPaymentInputView
import io.snabble.sdk.ui.utils.serializableExtra
import io.snabble.sdk.ui.utils.setTextOrHide

class PaymentSelectionDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.snabble_dialog_payment_selection, container, false)
            .apply { arguments?.applyTo(this) }

    private fun Bundle.applyTo(view: View): View {
        val optionsLayout = view.findViewById<LinearLayout>(R.id.options)

        val headerView = View.inflate(view.context, R.layout.snabble_item_payment_select_header, null)
        optionsLayout.addView(headerView, MATCH_PARENT, WRAP_CONTENT)

        if (getBoolean(ARG_SHOW_OFFLINE_HINT, false)) {
            val offlineView = View.inflate(view.context, R.layout.snabble_item_payment_select_offline_hint, null)
            optionsLayout.addView(offlineView, MATCH_PARENT, WRAP_CONTENT)
        }

        if (containsKey(ARG_ENTRIES)) addPaymentMethodEntryViews(optionsLayout, arguments = this)

        return view
    }

    private fun addPaymentMethodEntryViews(optionsLayout: LinearLayout, arguments: Bundle) {
        val entries: List<PaymentSelectionHelper.Entry> = arguments.serializableExtra<ArrayList<*>>(ARG_ENTRIES)
            ?.mapNotNull { it as? PaymentSelectionHelper.Entry }
            ?: return

        val selectedEntry: PaymentSelectionHelper.Entry? = arguments.serializableExtra(ARG_SELECTED_ENTRY)
        val hasAnyAddedMethods = entries.any { it.isAdded }
        entries.forEach { entry ->
            setupPaymentMethodEntryView(entry, hasAnyAddedMethods, selectedEntry, optionsLayout)
        }
    }

    private fun setupPaymentMethodEntryView(
        entry: PaymentSelectionHelper.Entry,
        hasAnyAddedMethods: Boolean,
        selectedEntry: PaymentSelectionHelper.Entry?,
        optionsLayout: LinearLayout
    ) {
        val paymentOptionView = View.inflate(optionsLayout.context, R.layout.snabble_item_payment_select, null)
        val imageView = paymentOptionView.findViewById<ImageView>(R.id.image)
        val name = paymentOptionView.findViewById<TextView>(R.id.name)
        val id = paymentOptionView.findViewById<TextView>(R.id.id)
        val check = paymentOptionView.findViewById<View>(R.id.check)

        paymentOptionView.accessibility {
            if (entry.isAdded || entry.paymentMethod.isOfflineMethod) {
                setClickAction(R.string.Snabble_Shoppingcart_Accessibility_actionUse)
            } else {
                setClickAction(R.string.Snabble_Shoppingcart_Accessibility_actionAdd)
            }
        }

        val resId = entry.iconResId
        if (resId != 0) {
            imageView.setImageResource(entry.iconResId)
        } else {
            imageView.visibility = View.INVISIBLE
        }

        if (entry.isAdded || !hasAnyAddedMethods) {
            imageView.colorFilter = null
        } else {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            val cf = ColorMatrixColorFilter(matrix)
            imageView.colorFilter = cf
        }

        id.setTextOrHide(entry.hint)
        name.setTextOrHide(entry.text)
        val endsWithNumber = "^.*?(\\d+)\$".toRegex()
        if (entry.hint?.matches(endsWithNumber) == true) {
            endsWithNumber.find(entry.hint)?.groupValues?.last()?.let { number ->
                id.contentDescription = resources.getString(
                    R.string.Snabble_Shoppingcart_Accessibility_cardEndsWith,
                    number
                )
            }
        }
        name.isEnabled = entry.isAvailable
        if (entry.isAvailable) {
            paymentOptionView.setOnClickListener {
                if (entry.isAdded) {
                    PaymentSelectionHelper.getInstance().select(entry)
                    dismissAllowingStateLoss()
                } else {
                    openPaymentInputView(
                        requireContext(),
                        entry.paymentMethod,
                        requireNotNull(Snabble.checkedInProject.value).id
                    )
                    dismissAllowingStateLoss()
                }
            }
        }
        check.isVisible = entry == selectedEntry
        optionsLayout.addView(paymentOptionView, MATCH_PARENT, WRAP_CONTENT)
    }

    override fun onPause() {
        super.onPause()

        dismissAllowingStateLoss()
    }

    companion object {

        const val ARG_ENTRIES = "entries"
        const val ARG_SHOW_OFFLINE_HINT = "showOfflineHint"
        const val ARG_SELECTED_ENTRY = "selectedEntry"
    }
}

package io.snabble.sdk.ui.cart

import io.snabble.sdk.ui.payment.PaymentInputViewHelper.openPaymentInputView
import io.snabble.sdk.ui.SnabbleUI.project
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import io.snabble.sdk.ui.R
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.snabble.sdk.ui.accessibility
import io.snabble.sdk.ui.utils.setTextOrHide
import java.util.ArrayList

class PaymentSelectionDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = View.inflate(requireContext(), R.layout.snabble_dialog_payment_selection, null)
        val options = view.findViewById<LinearLayout>(R.id.options)
        arguments?.let { args ->
            val headerView = View.inflate(requireContext(), R.layout.snabble_item_payment_select_header, null)
            options.addView(headerView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            if (args.getBoolean(ARG_SHOW_OFFLINE_HINT, false)) {
                val v = View.inflate(requireContext(), R.layout.snabble_item_payment_select_offline_hint, null)
                options.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            if (args.containsKey(ARG_ENTRIES)) {
                val selectedEntry = args.getSerializable(ARG_SELECTED_ENTRY) as PaymentSelectionHelper.Entry?
                (args.getSerializable(ARG_ENTRIES) as? ArrayList<PaymentSelectionHelper.Entry>)?.let { entries ->
                    val hasAnyAddedMethods = entries.any { it.isAdded }
                    entries.forEach { entry ->
                        val v = View.inflate(requireContext(), R.layout.snabble_item_payment_select, null)
                        val imageView = v.findViewById<ImageView>(R.id.image)
                        val name = v.findViewById<TextView>(R.id.name)
                        val id = v.findViewById<TextView>(R.id.id)
                        val check = v.findViewById<View>(R.id.check)

                        v.accessibility {
                            if (entry.isAdded) {
                                setClickAction(R.string.Snabble_Shoppingcart_Accessibility_actionAdd)
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
                                id.contentDescription = resources.getString(R.string.Snabble_Shoppingcart_Accessibility_cardEndsWith, number)
                            }
                        }
                        name.isEnabled = entry.isAvailable
                        if (entry.isAvailable) {
                            v.setOnClickListener {
                                if (entry.isAdded) {
                                    PaymentSelectionHelper.getInstance().select(entry)
                                    dismissAllowingStateLoss()
                                } else {
                                    openPaymentInputView(requireContext(), entry.paymentMethod, project.id)
                                    dismissAllowingStateLoss()
                                }
                            }
                        }
                        check.isVisible = entry == selectedEntry
                        options.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                }
            }
        }
        return view
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
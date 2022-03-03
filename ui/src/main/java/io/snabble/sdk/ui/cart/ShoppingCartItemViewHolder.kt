package io.snabble.sdk.ui.cart

import android.widget.TextView
import android.widget.EditText
import android.text.TextWatcher
import android.annotation.SuppressLint
import android.content.Context
import io.snabble.sdk.ui.cart.ShoppingCartView.ProductRow
import android.content.res.ColorStateList
import android.graphics.Color
import io.snabble.sdk.Product
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.OneShotClickListener
import android.text.Editable
import android.text.InputFilter
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.snabble.sdk.ui.utils.InputFilterMinMax
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.setOrHide
import java.lang.NumberFormatException

class ShoppingCartItemViewHolder internal constructor(
    itemView: View,
    private val undoHelper: UndoHelper
) : RecyclerView.ViewHolder(itemView) {
    var image: ImageView = itemView.findViewById(R.id.helper_image)
    var name: TextView = itemView.findViewById(R.id.name)
    var subtitle: TextView = itemView.findViewById(R.id.subtitle)
    var quantityTextView: TextView = itemView.findViewById(R.id.quantity)
    var priceTextView: TextView = itemView.findViewById(R.id.price)
    var plus: View = itemView.findViewById(R.id.plus)
    var minus: View = itemView.findViewById(R.id.minus)
    var quantityEdit: EditText = itemView.findViewById(R.id.quantity_edit)
    var controlsUserWeighed: View = itemView.findViewById(R.id.controls_user_weighed)
    var controlsDefault: View = itemView.findViewById(R.id.controls_default)
    var quantityEditApply: View = itemView.findViewById(R.id.quantity_edit_apply)
    var quantityEditApplyLayout: View = itemView.findViewById(R.id.quantity_edit_apply_layout)
    var quantityAnnotation: TextView = itemView.findViewById(R.id.quantity_annotation)
    var redLabel: TextView = itemView.findViewById(R.id.red_label)
    var textWatcher: TextWatcher? = null
    private val picasso = Picasso.get()

    @SuppressLint("SetTextI18n")
    internal fun bindTo(row: ProductRow, hasAnyImages: Boolean) {
        name.setOrHide(row.name)
        priceTextView.setOrHide(row.priceText)
        quantityTextView.setOrHide(row.quantityText)
        if (row.imageUrl != null) {
            image.visibility = View.VISIBLE
            picasso.load(row.imageUrl).into(image)
        } else {
            image.visibility = if (hasAnyImages) View.INVISIBLE else View.GONE
            image.setImageBitmap(null)
        }
        val hasCoupon = row.item.coupon != null
        var isAgeRestricted = false
        redLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ff0000"))
        if (row.item.product != null) {
            isAgeRestricted = row.item.product!!.saleRestriction.isAgeRestriction
        }
        redLabel.visibility = if (hasCoupon || isAgeRestricted) View.VISIBLE else View.GONE
        if (hasCoupon) {
            if (!row.manualDiscountApplied) {
                redLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#999999"))
            }
            redLabel.text = "%"
        } else {
            val age = row.item.product!!.saleRestriction.value
            if (age > 0) {
                redLabel.text = age.toString()
            } else {
                redLabel.visibility = View.GONE
            }
        }
        var encodingDisplayValue = "g"
        val encodingUnit = row.encodingUnit
        if (encodingUnit != null) {
            encodingDisplayValue = encodingUnit.displayValue
        }
        quantityAnnotation.text = encodingDisplayValue
        if (row.editable) {
            if (row.item.product!!.type == Product.Type.UserWeighed) {
                controlsDefault.visibility = View.GONE
                controlsUserWeighed.visibility = View.VISIBLE
            } else {
                controlsDefault.visibility = View.VISIBLE
                controlsUserWeighed.visibility = View.GONE
            }
        } else {
            controlsDefault.visibility = View.GONE
            controlsUserWeighed.visibility = View.GONE
        }
        plus.setOnClickListener { v: View? ->
            row.item.quantity = row.item.quantity + 1
            Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.product)
        }
        minus.setOnClickListener { v: View? ->
            val p = bindingAdapterPosition
            val newQuantity = row.item.quantity - 1
            if (newQuantity <= 0) {
                undoHelper.removeAndShowUndoSnackbar(p, row.item)
            } else {
                row.item.quantity = newQuantity
                Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.product)
            }
        }
        quantityEditApply.setOnClickListener(object : OneShotClickListener() {
            override fun click() {
                row.item.quantity = quantityEditValue
                hideInput()
                Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.product)
            }
        })
        quantityEdit.setText(row.quantity.toString())
        itemView.isFocusable = true
        itemView.isFocusableInTouchMode = true
        if (bindingAdapterPosition == 0) {
            itemView.requestFocus()
        }
        quantityEdit.removeTextChangedListener(textWatcher)
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updateQuantityEditApplyVisibility(row.quantity)
            }

            override fun afterTextChanged(s: Editable) {}
        }
        updateQuantityEditApplyVisibility(row.quantity)
        quantityEdit.addTextChangedListener(textWatcher)
        quantityEdit.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                quantityEditApply.callOnClick()
                return@setOnEditorActionListener true
            }
            false
        }
        quantityEdit.filters = arrayOf<InputFilter>(InputFilterMinMax(0, ShoppingCart.MAX_QUANTITY))
    }

    fun hideInput() {
        val imm = quantityEdit.context
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            quantityEdit.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
        quantityEdit.clearFocus()
    }

    private fun updateQuantityEditApplyVisibility(quantity: Int) {
        val value = quantityEditValue
        if (value > 0 && value != quantity) {
            quantityEditApply.visibility = View.VISIBLE
            quantityEditApplyLayout.visibility = View.VISIBLE
        } else {
            quantityEditApply.visibility = View.GONE
            quantityEditApplyLayout.visibility = View.GONE
        }
    }

    val quantityEditValue: Int
        get() = try {
            quantityEdit.text.toString().toInt()
        } catch (e: NumberFormatException) {
            0
        }
}
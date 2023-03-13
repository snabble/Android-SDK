package io.snabble.sdk.ui.cart

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import io.snabble.accessibility.accessibility
import io.snabble.accessibility.orderViewsForAccessibility
import io.snabble.sdk.Product
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.ShoppingCartView.ProductRow
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.InputFilterMinMax
import io.snabble.sdk.ui.utils.setOneShotClickListener
import io.snabble.sdk.ui.utils.setTextOrHide

class ShoppingCartItemViewHolder internal constructor(
    itemView: View,
    private val undoHelper: UndoHelper
) : RecyclerView.ViewHolder(itemView) {

    var image: ImageView = itemView.findViewById(R.id.helper_image)
    var name: TextView = itemView.findViewById(R.id.name)
    var subtitle: TextView? = itemView.findViewById(R.id.subtitle)
    var quantityTextView: TextView = itemView.findViewById(R.id.quantity)
    var priceTextView: TextView = itemView.findViewById(R.id.price)
    var plus: View = itemView.findViewById(R.id.plus)
    var minus: MaterialButton = itemView.findViewById(R.id.minus)
    var quantityEdit: EditText = itemView.findViewById(R.id.quantity_edit)
    var controlsUserWeighed: View = itemView.findViewById(R.id.controls_user_weighed)
    var controlsDefault: View = itemView.findViewById(R.id.controls_default)
    var quantityEditApply: View = itemView.findViewById(R.id.quantity_edit_apply)
    var quantityEditApplyLayout: View = itemView.findViewById(R.id.quantity_edit_apply_layout)
    var quantityAnnotation: TextView = itemView.findViewById(R.id.quantity_annotation)
    var redLabel: TextView = itemView.findViewById(R.id.red_label)
    private val picasso = Picasso.get()

    init {
        orderViewsForAccessibility(quantityTextView, quantityEdit, subtitle, name, redLabel, priceTextView, minus, plus)
    }

    @SuppressLint("SetTextI18n")
    fun bindTo(row: ProductRow, hasAnyImages: Boolean) {
        val res = itemView.resources
        itemView.accessibility {
            if (isTalkBackActive) {
                setLongClickAction(R.string.Snabble_Shoppingcart_Accessibility_actionDelete) {
                    Snabble.checkedInProject.value?.shoppingCart?.remove(bindingAdapterPosition)
                }
            }
            onInitializeAccessibilityNodeInfo { info ->
                info.text = res.getString(R.string.Snabble_Shoppingcart_Accessibility_contextInCart)
            }
        }
        name.setTextOrHide(row.name)
        priceTextView.setTextOrHide(row.priceText)
        quantityTextView.setTextOrHide(row.quantityText)
        row.quantityText?.let {
            quantityTextView.contentDescription = res.getString(R.string.Snabble_Shoppingcart_Accessibility_descriptionQuantity, it)
        }
        row.priceText?.let {
            priceTextView.contentDescription = res.getString(R.string.Snabble_Shoppingcart_Accessibility_descriptionForPrice, it)
        }
        if (row.imageUrl != null) {
            image.visibility = View.VISIBLE
            picasso.load(row.imageUrl).into(image)
        } else {
            image.isVisible = hasAnyImages
            image.setImageBitmap(null)
        }
        val hasCoupon = row.item.coupon != null
        val isAgeRestricted = row.item.product?.saleRestriction?.isAgeRestriction ?: false
        redLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ff0000"))
        redLabel.isVisible = hasCoupon || isAgeRestricted
        if (hasCoupon) {
            if (!row.manualDiscountApplied) {
                redLabel.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#999999"))
                redLabel.contentDescription = res.getString(R.string.Snabble_Shoppingcart_Accessibility_descriptionWithoutDiscount)
            } else {
                redLabel.contentDescription = res.getString(R.string.Snabble_Shoppingcart_Accessibility_descriptionWithDiscount)
            }
            redLabel.text = "%"
        } else {
            val age = row.item.product?.saleRestriction?.value ?: 0
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
        controlsDefault.isVisible = row.editable && row.item.product?.type != Product.Type.UserWeighed
        controlsUserWeighed.isVisible = row.editable && row.item.product?.type == Product.Type.UserWeighed
        plus.setOnClickListener {
            row.item.quantity++

            updateMinusButtonIcon(row.item.quantity)

            Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.product)
        }
        updateMinusButtonIcon(row.item.quantity)
        minus.setOnClickListener {
            val p = bindingAdapterPosition
            val newQuantity = row.item.quantity - 1
            if (newQuantity <= 0) {
                undoHelper.removeAndShowUndoSnackbar(p, row.item)
            } else {
                row.item.quantity = newQuantity
                Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.product)
            }

            updateMinusButtonIcon(newQuantity)
        }
        quantityEditApply.setOneShotClickListener {
            row.item.quantity = quantityEditValue
            hideInput()
            Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.product)
        }
        quantityEdit.setText(row.quantity.toString())
        itemView.isFocusable = true
        itemView.isFocusableInTouchMode = true
        if (bindingAdapterPosition == 0) {
            itemView.requestFocus()
        }
        updateQuantityEditApplyVisibility(row.quantity)
        quantityEdit.addTextChangedListener(onTextChanged = { _, _, _, _ ->
            updateQuantityEditApplyVisibility(row.quantity)
        })
        quantityEdit.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event.action == KeyEvent.ACTION_DOWN
                        && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                quantityEditApply.callOnClick()
                return@setOnEditorActionListener true
            }
            false
        }
        quantityEdit.filters = arrayOf(InputFilterMinMax(0, ShoppingCart.MAX_QUANTITY))
    }

    private fun updateMinusButtonIcon(quantity: Int) {
        val iconRes = if (quantity == 1) R.drawable.snabble_ic_delete else R.drawable.snabble_ic_minus
        minus.icon = AppCompatResources.getDrawable(minus.context, iconRes)
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

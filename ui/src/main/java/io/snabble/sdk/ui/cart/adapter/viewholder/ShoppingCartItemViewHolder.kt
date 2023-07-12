package io.snabble.sdk.ui.cart.adapter.viewholder

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
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
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
import io.snabble.sdk.Unit
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.UndoHelper
import io.snabble.sdk.ui.cart.adapter.ProductRow
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.InputFilterMinMax
import io.snabble.sdk.ui.utils.inputMethodManager
import io.snabble.sdk.ui.utils.setOneShotClickListener
import io.snabble.sdk.ui.utils.setTextOrHide

class ShoppingCartItemViewHolder internal constructor(
    itemView: View,
    private val undoHelper: UndoHelper
) : RecyclerView.ViewHolder(itemView) {

    val image: ImageView = itemView.findViewById(R.id.helper_image)
    val name: TextView = itemView.findViewById(R.id.name)
    val minus: MaterialButton = itemView.findViewById(R.id.minus)
    val plus: View = itemView.findViewById(R.id.plus)
    private val controlsDefault: View = itemView.findViewById(R.id.controls_default)
    private val controlsUserWeighed: View = itemView.findViewById(R.id.controls_user_weighed)
    private val controlsUserWeighedDelete: View = itemView.findViewById(R.id.delete_weighed)
    private val priceTextView: TextView = itemView.findViewById(R.id.price)
    private val quantityAnnotation: TextView = itemView.findViewById(R.id.quantity_annotation)
    private val quantityEdit: EditText = itemView.findViewById(R.id.quantity_edit)
    private val quantityEditApply: View = itemView.findViewById(R.id.quantity_edit_apply)
    private val quantityEditApplyLayout: View = itemView.findViewById(R.id.quantity_edit_apply_layout)
    private val quantityTextView: TextView = itemView.findViewById(R.id.quantity)
    private val redLabel: TextView = itemView.findViewById(R.id.red_label)
    private val subtitle: TextView? = itemView.findViewById(R.id.subtitle)

    private val picasso: Picasso = Picasso.get()

    private val context: Context get() = itemView.context

    init {
        orderViewsForAccessibility(quantityTextView, quantityEdit, subtitle, name, redLabel, priceTextView, minus, plus)
    }

    fun bindTo(product: ProductRow, hasAnyImages: Boolean) {
        setupAccessibility()

        name.setTextOrHide(product.name)

        priceTextView.setTextOrHide(product.priceText)
        priceTextView.contentDescription(
            predicate = { product.priceText != null },
            stringRes = R.string.Snabble_Shoppingcart_Accessibility_descriptionForPrice,
            product.priceText
        )

        product.encodingUnit

        quantityTextView.setTextOrHide(product.quantityText)
        quantityTextView.contentDescription(
            predicate = { product.quantityText != null },
            stringRes = R.string.Snabble_Shoppingcart_Accessibility_descriptionQuantity,
            product.quantityText
        )

        image.setImage(product.imageUrl, hasAnyImages)

        setupReducedPriceLabel(product)

        quantityAnnotation.text = getQuantityUnit(product.encodingUnit)

        setupUserWeighedQuantityControls(product)

        setupQuantityButtons(product)

        setupQuantityEditView(product)
    }

    private fun setupAccessibility() {
        itemView.accessibility {
            if (isTalkBackActive) {
                setLongClickAction(R.string.Snabble_Shoppingcart_Accessibility_actionDelete) {
                    Snabble.checkedInProject.value?.shoppingCart?.remove(bindingAdapterPosition)
                }
            }
            onInitializeAccessibilityNodeInfo { info ->
                info.text = context.getString(R.string.Snabble_Shoppingcart_Accessibility_contextInCart)
            }
        }
    }

    private fun ImageView.setImage(imageUrl: String?, hasAnyImages: Boolean) {
        if (imageUrl != null) {
            visibility = View.VISIBLE
            picasso.load(imageUrl).into(this)
        } else {
            isVisible = hasAnyImages
            setImageBitmap(null)
        }
    }

    private fun setupReducedPriceLabel(product: ProductRow) = redLabel.apply {
        val hasCoupon = product.item?.coupon != null

        val labelStringRes: String? = getReducedPriceLabelText(hasCoupon, getAgeRestrictionValue(product))
        setTextOrHide(labelStringRes)

        val backgroundTintColor: Int = getReducePriceLabelBackgroundColor(hasCoupon, product.manualDiscountApplied)
        backgroundTintList = ColorStateList.valueOf(backgroundTintColor)

        val contentDescriptionResId: Int? =
            getReducedPriceLabelContentDescription(hasCoupon, product.manualDiscountApplied)
        contentDescription = contentDescriptionResId?.let(context::getString)
    }

    private fun getReducedPriceLabelText(hasCoupon: Boolean, ageRestrictionValue: String?): String? =
        if (hasCoupon) DISCOUNT_SYMBOL else ageRestrictionValue

    private fun getAgeRestrictionValue(product: ProductRow): String? = product.item?.product
        ?.saleRestriction
        ?.value
        ?.takeIf { it > 0 }
        ?.toString()

    @ColorInt
    private fun getReducePriceLabelBackgroundColor(
        hasCoupon: Boolean,
        isManualDiscountApplied: Boolean,
    ): Int =
        if (hasCoupon && !isManualDiscountApplied) {
            DISCOUNT_BACKGROUND_COLOR
        } else {
            MANUAL_DISCOUNT_BACKGROUND_COLOR
        }
            .let(Color::parseColor)

    private fun getQuantityUnit(unit: Unit?): String = unit?.displayValue ?: UNIT_SYMBOL

    private fun setupUserWeighedQuantityControls(product: ProductRow) {
        val isUserWeighed = product.editable && product.item?.product?.type == Product.Type.UserWeighed

        controlsDefault.isVisible = !isUserWeighed
        controlsUserWeighed.isVisible = isUserWeighed

        controlsUserWeighedDelete.setOnClickListener {
            undoHelper.removeAndShowUndoSnackbar(bindingAdapterPosition, product.item)
        }
    }

    private fun setupQuantityButtons(product: ProductRow) {
        updateMinusButtonIcon(product.item?.quantity ?: 1) // TODO: Refactor
        minus.setOnClickListener {
            val newQuantity = product.item?.quantity?.minus(1) ?: 0 // TODO: Refactor
            if (newQuantity <= 0) {
                undoHelper.removeAndShowUndoSnackbar(bindingAdapterPosition, product.item)
            } else {
                product.item?.quantity = newQuantity
                Telemetry.event(Telemetry.Event.CartAmountChanged, product.item?.product)
            }

            updateMinusButtonIcon(newQuantity)
        }

        plus.setOnClickListener {
            product.item?.quantity = product.item?.quantity?.plus(1) ?: 1 // TODO: Refactor

            updateMinusButtonIcon(product.item?.quantity ?: 1) // TODO: Refactor

            Telemetry.event(Telemetry.Event.CartAmountChanged, product.item?.product)
        }
    }

    private fun setupQuantityEditView(product: ProductRow) {
        quantityEditApply.setOneShotClickListener {
            product.item?.quantity = quantityEdit.getTextAsNumericValue() ?: 0
            hideInput()
            quantityEdit.clearFocus()
            Telemetry.event(Telemetry.Event.CartAmountChanged, product.item?.product)
        }

        quantityEdit.setText(product.quantity.toString())
        updateQuantityEditApplyVisibility(product.quantity)
        quantityEdit.addTextChangedListener(onTextChanged = { _, _, _, _ ->
            updateQuantityEditApplyVisibility(product.quantity)
        })
        quantityEdit.setOnEditorActionListener { _, actionId, event ->
            // TODO: Why different actions? Our keyboard mode?
            if (actionId == EditorInfo.IME_ACTION_DONE
                || (event.action == KeyEvent.ACTION_DOWN
                    && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                quantityEditApply.callOnClick()
                true
            } else {
                false
            }
        }
        quantityEdit.filters = arrayOf(InputFilterMinMax(0, ShoppingCart.MAX_QUANTITY))
    }

    private fun updateMinusButtonIcon(quantity: Int) {
        val iconRes = if (quantity == 1) R.drawable.snabble_ic_delete else R.drawable.snabble_ic_minus
        minus.icon = AppCompatResources.getDrawable(minus.context, iconRes)
    }

    fun hideInput() {
        context.inputMethodManager.hideSoftInputFromWindow(
            quantityEdit.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun updateQuantityEditApplyVisibility(quantity: Int) {
        val newQuantity = quantityEdit.getTextAsNumericValue() ?: 0
        val isNewQuantityValid = newQuantity > 0 && newQuantity != quantity
        quantityEditApply.isVisible = isNewQuantityValid
        quantityEditApplyLayout.isVisible = isNewQuantityValid
    }

    private companion object {

        const val UNIT_SYMBOL = "g"

        const val DISCOUNT_SYMBOL = "%"

        const val DISCOUNT_BACKGROUND_COLOR = "#999999"
        const val MANUAL_DISCOUNT_BACKGROUND_COLOR = "#ff0000"
    }
}

@StringRes
private fun getReducedPriceLabelContentDescription(
    hasCoupon: Boolean,
    isManualDiscountApplied: Boolean
): Int? =
    if (hasCoupon) {
        if (isManualDiscountApplied) {
            R.string.Snabble_Shoppingcart_Accessibility_descriptionWithDiscount
        } else {
            R.string.Snabble_Shoppingcart_Accessibility_descriptionWithoutDiscount
        }
    } else {
        null
    }

private fun View.contentDescription(
    predicate: () -> Boolean,
    @StringRes stringRes: Int,
    vararg args: String,
) {
    contentDescription = if (predicate()) context.getString(stringRes, *args) else null
}

private fun TextView.getTextAsNumericValue(): Int? =
    try {
        text.toString().toInt()
    } catch (e: NumberFormatException) {
        null
    }

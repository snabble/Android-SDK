package io.snabble.sdk.ui.cart.shoppingcart.adapter

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.Product
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.UndoHelper
import io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.AgeRstriction
import io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.ImageWidget
import io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.PriceDescription
import io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.QuantityWidget
import io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.UserWeighed
import io.snabble.sdk.ui.cart.shoppingcart.row.ProductRow
import io.snabble.sdk.ui.cart.shoppingcart.row.SimpleRow
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.ThemeWrapper

class LineItemViewHolder(private val composeView: ComposeView, private val undoHelper: UndoHelper) :
    RecyclerView.ViewHolder(composeView) {

    fun bind(row: ProductRow, hasAnyImages: Boolean) {
        val hasCoupon = (row.item?.coupon != null)
        val isAgeRestricted = row.item?.product?.saleRestriction?.isAgeRestriction ?: false
        val age = row.item?.product?.saleRestriction?.value ?: 0
        val encodingUnit = row.encodingUnit
        val encodingUnitDisplayName = encodingUnit?.displayValue ?: "g"
        composeView.setContent {
            ThemeWrapper {
                Row(
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageWidget(row, hasAnyImages)
                    AgeRstriction(hasCoupon, isAgeRestricted, age, row.manualDiscountApplied)
                    PriceDescription(row)
                    Spacer(modifier = Modifier.weight(1f))
                    if (row.editable && row.item?.product?.type != Product.Type.UserWeighed) {
                        QuantityWidget(row, onQuantityChanged = {
                            if (it <= 0) {
                                undoHelper.removeAndShowUndoSnackbar(bindingAdapterPosition, row.item)
                            } else {
                                row.item?.setQuantityMethod(it)
                                Telemetry.event(Telemetry.Event.CartAmountChanged, row.item?.product)
                            }
                        })
                    }
                    if (row.editable && row.item?.product?.type == Product.Type.UserWeighed) {
                        UserWeighed(
                            row.quantity.toString(),
                            encodingUnitDisplayName,
                            onQuantityChanged = {
                                row.item.setQuantityMethod(it)
                                Telemetry.event(Telemetry.Event.CartAmountChanged, row.item.product)
                            },
                            onDeleteWeighed = {
                                undoHelper.removeAndShowUndoSnackbar(
                                    bindingAdapterPosition,
                                    row.item
                                )
                            })
                    }
                }
            }
        }
    }

    fun bind(row: SimpleRow, hasAnyImages: Boolean) {
        val imageResId = if (row.imageResId == 0) R.drawable.snabble_ic_deposit else row.imageResId
        composeView.setContent {
            ThemeWrapper {
                Row(
                    Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasAnyImages) {
                        Image(
                            modifier = Modifier.size(44.dp),
                            painter = painterResource(id = imageResId),
                            contentDescription = row.title,
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                    }
                    ItemDesciption(title = row.title, price = row.text)

                }
            }
        }
    }
}

@Composable
fun ItemDesciption(
    modifier: Modifier = Modifier,
    title: String?,
    price: String?
) {
    Column(modifier = modifier) {
        title?.let {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        price?.let {
            Text(text = price, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

class TextFieldManager constructor(
    private val focusManager: FocusManager,
    private val keyboardController: SoftwareKeyboardController?,
) {

    fun clearFocusAndHideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun moveFocusToNext() {
        focusManager.moveFocus(FocusDirection.Next)
    }

    fun clearFocus() {
        focusManager.clearFocus()
    }
}

@Composable
fun rememberTextFieldManager(): TextFieldManager {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return TextFieldManager(focusManager, keyboardController)
}

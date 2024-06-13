package io.snabble.sdk.ui.cart.shoppingcart.adapter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import io.snabble.sdk.Product
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.UndoHelper
import io.snabble.sdk.ui.cart.shoppingcart.row.new.ProductRow
import io.snabble.sdk.ui.cart.shoppingcart.row.new.SimpleRow
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.ThemeWrapper

class LineItemViewHolder(private val composeView: ComposeView, private val undoHelper: UndoHelper) :
    RecyclerView.ViewHolder(composeView) {

    fun bind(row: ProductRow, hasAnyImages: Boolean) {
        val hasCoupon = row.item?.coupon != null
        val isAgeRestricted = row.item?.product?.saleRestriction?.isAgeRestriction ?: false
        val age = row.item?.product?.saleRestriction?.value ?: 0
        val encodingUnit = row.encodingUnit
        val encodingUnitDisplayName = encodingUnit?.displayValue ?: "g"
        composeView.setContent {
            ThemeWrapper {

                Row(
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 8.dp)
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageWidget(row, hasAnyImages)
                    AgeRstriction(hasCoupon, isAgeRestricted, age)
                    PriceDescription(row)
                    Spacer(modifier = Modifier.weight(1f))
//                QuantityWidget(row)
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

    @Composable
    private fun UserWeighed(
        weight: String,
        quantityAnnotation: String,
        onQuantityChanged: (Int) -> Unit,
        onDeleteWeighed: () -> Unit
    ) {
        var value by remember {
            mutableStateOf(TextFieldValue(weight))
        }

        var showApplyButton by remember {
            mutableStateOf(false)
        }

        val textFieldManager = rememberTextFieldManager()
        if (weight.toInt() > 0) {
            Row(
                modifier = Modifier.widthIn(min = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    modifier = Modifier.size(36.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)
                    ),
                    onClick = { onDeleteWeighed() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.snabble_ic_delete),
                        contentDescription = stringResource(
                            id = R.string.Snabble_Shoppingcart_Accessibility_actionDelete
                        ),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.width(96.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f),
                    ),
                    placeholder = {
                        Text(text = stringResource(id = R.string.Snabble_Shoppingcart_Accessibility_quantity))
                    },
                    value = value,
                    onValueChange = {
                        if (it.text.isNotBlank() &&
                            it.text.toInt() > 0 &&
                            it.text.toInt() < ShoppingCart.MAX_QUANTITY
                        ) {
                            if (it.text != value.text) showApplyButton = true
                            value = it
                        }
                    },
                    keyboardActions = KeyboardActions(onDone = {
                        onQuantityChanged(value.text.toInt())
                        showApplyButton = false
                        textFieldManager.clearFocusAndHideKeyboard()
                    }),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Text(text = quantityAnnotation, style = MaterialTheme.typography.bodyMedium)
                if (showApplyButton) {
                    OutlinedIconButton(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)
                        ),
                        onClick = {
                            onQuantityChanged(value.text.toInt())
                            showApplyButton = false
                            textFieldManager.clearFocusAndHideKeyboard()
                        }) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(6.dp),
                            painter = painterResource(id = R.drawable.snabble_ic_check_white),
                            contentDescription = stringResource(
                                id = R.string.Snabble_Shoppingcart_Accessibility_actionDelete
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AgeRstriction(hasCoupon: Boolean, isAgeRestricted: Boolean, age: Long) {
        if (hasCoupon || (isAgeRestricted && age > 0)) {
            Text(
                modifier = Modifier.background(Color.Red),
                text = if (hasCoupon) "%" else age.toString(),
                color = Color.White
            )
        }
    }

    @Composable
    @OptIn(ExperimentalGlideComposeApi::class)
    private fun ImageWidget(row: ProductRow, hasAnyImages: Boolean) {
        if (row.imageUrl != null) {
            GlideImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)), model = row.imageUrl, contentDescription = row.name
            )
        } else if (hasAnyImages) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }

    @Composable
    private fun QuantityWidget(row: ProductRow) {
        row.quantityText?.let {
            Box(modifier = Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier.height(38.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    OutlinedIconButton(
                        modifier = Modifier.size(36.dp),
                        onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.snabble_ic_minus),
                            contentDescription = stringResource(
                                id = R.string.Snabble_Shoppingcart_Accessibility_decreaseQuantity
                            ),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        modifier = Modifier.widthIn(min = 36.dp),
                        text = it,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedIconButton(
                        modifier = Modifier.size(36.dp),
                        onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.snabble_ic_add),
                            contentDescription = stringResource(
                                id = R.string.Snabble_Shoppingcart_Accessibility_increaseQuantity
                            ),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PriceDescription(row: ProductRow) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            row.name?.let {
                Text(
                    text = it,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            row.priceText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    fun bind(row: SimpleRow, hasAnyImages: Boolean) {
        composeView.setContent {
            Row {
                if (hasAnyImages) {
                    Image(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(id = row.imageResId),
                        contentDescription = row.title
                    )
                }
                ItemDesciption(title = row.title, price = row.text)

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

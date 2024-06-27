package io.snabble.sdk.ui.cart.shoppingcart.adapter

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.Product
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.UndoHelper
import io.snabble.sdk.ui.cart.shoppingcart.product.widget.ExtraImage
import io.snabble.sdk.ui.cart.shoppingcart.product.widget.UserWeighedField
import io.snabble.sdk.ui.cart.shoppingcart.row.SimpleRow
import io.snabble.sdk.ui.cart.shoppingcart.row.ProductRow
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
                Column {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 16.dp, horizontal = 16.dp)
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.wrapContentSize()) {
//                            ItemImage(row, hasAnyImages)
                            ExtraImage(hasCoupon, isAgeRestricted, age, row.manualDiscountApplied)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row {

//                                PriceDescription(modifier = Modifier.weight(1f), row)
//                                if (row.editable && row.item?.product?.type != Product.Type.UserWeighed) {
//                                    QuantityField(modifier = Modifier, row, onQuantityChanged = {
//                                        if (it <= 0) {
//                                            undoHelper.removeAndShowUndoSnackbar(bindingAdapterPosition, row.item)
//                                        } else {
//                                            row.item?.setQuantityMethod(it)
//                                            Telemetry.event(Telemetry.Event.CartAmountChanged, row.item?.product)
//                                        }
//                                    })
//                                }
                                if (row.editable && row.item?.product?.type == Product.Type.UserWeighed) {
                                    UserWeighedField(
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
                            row.discounts.forEach {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(it.discount, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.weight(1f))
                                    Text(it.name, style = MaterialTheme.typography.bodyMedium)
                                    Image(
                                        modifier = Modifier.size(24.dp),
                                        painter = painterResource(R.drawable.discount_badge),
                                        contentDescription = ""
                                    )
                                }
                            }
                        }
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

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.title?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.discount?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            row.name?.let {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = row.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End
                                )
                                Image(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(R.drawable.discount_badge),
                                    contentDescription = ""
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

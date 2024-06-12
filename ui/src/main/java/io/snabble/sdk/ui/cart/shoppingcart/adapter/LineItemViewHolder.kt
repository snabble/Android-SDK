package io.snabble.sdk.ui.cart.shoppingcart.adapter

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import io.snabble.sdk.ui.cart.shoppingcart.row.new.ProductRow
import io.snabble.sdk.ui.cart.shoppingcart.row.new.SimpleRow

class LineItemViewHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {

    @OptIn(ExperimentalGlideComposeApi::class)
    fun bind(row: ProductRow, hasAnyImages: Boolean) {
        composeView.setContent {
            Row {
                Row {
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

                    ItemDesciption(title = row.name, price = row.quantityText)

                }
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

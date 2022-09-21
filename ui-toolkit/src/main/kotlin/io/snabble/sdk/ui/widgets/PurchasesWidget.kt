package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.flowlayout.FlowRow
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.ProjectId
import io.snabble.sdk.domain.PurchasesItem
import io.snabble.sdk.ui.toolkit.R

@Preview(
    backgroundColor = 0xEBEBEB,
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun PurchasesPreview() {
    PurchasesWidget(
        model = PurchasesItem(
            id = "last.purchases",
            projectId = ProjectId("0123"),
            padding = Padding(horizontal = 0)
        ),
        purchases = listOf(
            Purchase(amount = "13,37 €", title = "Snabble Store Bonn", time = "Today"),
            Purchase(
                amount = "7,56 €",
                title = "Snabble Store Bonn Dransdorf",
                time = "Yesterday"
            ),
            Purchase(
                amount = "42,08 €",
                title = "Snabble Store Bonn Bad Godesberg",
                time = "Two days Ago"
            ),
            Purchase(amount = "156,87 €", title = "Snabble Store Koblenz", time = "Last week"),
            Purchase(amount = "20,01 €", title = "Snabble Store London", time = "Last month"),
        ).subList(0, 5)
    )
}

@Composable
fun PurchasesWidget(
    model: PurchasesItem,
    purchases: List<Purchase>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = model.padding.bottom.dp)
    ) {
        Text(
            text = "Previous purchases",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(PaddingValues(horizontal = (model.padding.start + 8).dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = model.padding.start.dp,
                    end = model.padding.start.dp,
                    bottom = model.padding.start.dp,
                ),
            mainAxisSpacing = model.padding.start.dp,
            crossAxisSpacing = model.padding.start.dp,
        ) {
            purchases.forEach { data ->
                Purchase(data, padding = model.padding.start)
            }
        }
    }
}

data class Purchase(
    val amount: String,
    val title: String,
    val time: String,
)

@Preview(
    backgroundColor = 0xEBEBEB,
    showBackground = true,
)
@Composable
private fun PurchaseItemPreview() {
    Purchase(
        Purchase(
            amount = "7,56 €",
            title = "Snabble Store Bonn Dransdorf",
            time = "Yesterday",
        ),
        2,
        padding = 16,
    )
}

@OptIn(ExperimentalUnitApi::class)
@Composable
private fun Purchase(
    data: Purchase,
    columns: Int = 2,
    padding: Int,
) {
    Surface(
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        color = Color(Color.White.toArgb()),
    ) {
        ConstraintLayout(
            modifier = Modifier
                .width(calculateWidth(columns, padding).dp)
                .wrapContentHeight()
                .padding(PaddingValues(12.dp))
        ) {
            val (icon, amount, title, time) = createRefs()

            Image(
                painter = painterResource(id = R.drawable.ic_snabble),
                contentDescription = "",
                modifier = Modifier
                    .size(24.dp)
                    .constrainAs(icon) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
            )
            Text(
                text = data.amount,
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = TextUnit(-.21f, TextUnitType.Sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .constrainAs(amount) {
                        top.linkTo(icon.top)
                        bottom.linkTo(icon.bottom)
                        linkTo(start = icon.end, end = parent.end, bias = 1f)
                        width = Dimension.preferredWrapContent
                    }
            )
            Text(
                text = "${data.title}\n",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = TextUnit(-.24f, TextUnitType.Sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .constrainAs(title) {
                        top.linkTo(icon.bottom, margin = 12.dp)
                    }
            )
            Text(
                text = data.time,
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = TextUnit(-.21f, TextUnitType.Sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .constrainAs(time) {
                        top.linkTo(title.bottom, margin = 4.dp)
                    }
            )
        }
    }
}

@Composable
private fun calculateWidth(columns: Int, padding: Int): Float =
    LocalConfiguration.current.screenWidthDp / columns - padding * (1 + 1f / columns)

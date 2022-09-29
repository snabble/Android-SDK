package io.snabble.sdk.ui.widgets.purchase.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.ProjectId
import io.snabble.sdk.domain.PurchasesItem
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.widgets.purchase.OnLifecycleEvent
import io.snabble.sdk.ui.widgets.purchase.Purchase
import io.snabble.sdk.ui.widgets.purchase.viewmodel.Loading
import io.snabble.sdk.ui.widgets.purchase.viewmodel.PurchaseViewModel
import io.snabble.sdk.ui.widgets.purchase.viewmodel.ShowPurchases

@Composable
internal fun PurchaseScreen(
    model: PurchasesItem,
    viewModel: PurchaseViewModel = viewModel()
) {
    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) { _, _ ->
        viewModel.updatePurchases()
    }

    when (val state = viewModel.state) {
        Loading -> Unit
        is ShowPurchases -> {
            if (state.data.isNotEmpty()) {
                PurchasesWidget(model = model, purchaseList = state.data)
            }
        }
    }
}

@Composable
private fun PurchasesWidget(
    model: PurchasesItem,
    purchaseList: List<Purchase>,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = model.padding.bottom.dp)
    ) {
        val (title, more, purchases) = createRefs()
        Text(
            text = "Previous purchases",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(PaddingValues(horizontal = (model.padding.start + 8).dp))
                .constrainAs(title) {
                    linkTo(start = parent.start, end = more.start, bias = 0f)
                    top.linkTo(parent.top)
                    width = Dimension.preferredWrapContent
                }
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .constrainAs(more) {
                    end.linkTo(parent.end)
                    top.linkTo(title.top)
                    bottom.linkTo(title.bottom)
                    height = Dimension.fillToConstraints
                }
                .padding(PaddingValues(horizontal = (model.padding.start + 8).dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(
                        bounded = false,
                        color = MaterialTheme.colorScheme.primary
                    ),
                ) {

                }
        ) {
            Text(
                text = "More",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .constrainAs(purchases) {
                    start.linkTo(parent.start)
                    top.linkTo(title.bottom)
                }
        ) {
            purchaseList.forEachIndexed { index, purchase ->
                PurchaseDetail(
                    modifier = Modifier
                        .weight(1f),
                    data = purchase
                )
                if (index < purchaseList.lastIndex) {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalUnitApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDetail(
    modifier: Modifier = Modifier,
    data: Purchase,
) {
    CompositionLocalProvider(
        // TODO: Providing this app wide?
        LocalRippleTheme provides object : RippleTheme {

            @Composable
            override fun defaultColor(): Color = MaterialTheme.colorScheme.primary

            @Composable
            override fun rippleAlpha(): RippleAlpha =
                RippleTheme.defaultRippleAlpha(Color.Black, lightTheme = !isSystemInDarkTheme())
        }
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.small,
            onClick = {}
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
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
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .constrainAs(amount) {
                            top.linkTo(icon.top)
                            bottom.linkTo(icon.bottom)
                            linkTo(
                                start = icon.end,
                                end = parent.end,
                                bias = 1f,
                                startMargin = 4.dp
                            )
                            width = Dimension.fillToConstraints
                        }
                )
                Text(
                    text = "${data.title}\n\n",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .constrainAs(title) {
                            top.linkTo(icon.bottom, margin = 12.dp)
                        }
                )
                Text(
                    text = data.time,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
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
}

@Preview(backgroundColor = 0xEBEBEB, showBackground = true)
@Composable
private fun PurchaseDetailPreview() {
    PurchaseDetail(data = Purchase("7,56 €", "Snabble Store Bonn Dransdorf", "Yesterday"))
}

@Preview(backgroundColor = 0xEBEBEB, showBackground = true)
@Composable
private fun PurchaseWidgetPreview() {
    PurchasesWidget(
        model = PurchasesItem(
            id = "last.purchases",
            projectId = ProjectId("0123"),
            padding = Padding(horizontal = 0)
        ),
        purchaseList = listOf(Purchase("13,37 €", "Snabble Store Bonn", "Today"))
    )
}

@Preview(backgroundColor = 0xEBEBEB, showBackground = true)
@Composable
private fun TwoPurchasesPreview() {
    PurchasesWidget(
        model = PurchasesItem(
            id = "last.purchases",
            projectId = ProjectId("0123"),
            padding = Padding(horizontal = 0)
        ),
        purchaseList = listOf(
            Purchase("13,37 €", "Snabble Store Bonn", "Today"),
            Purchase("7,56 €", "Snabble Store Bonn Dransdorf", "Yesterday"),
        )
    )
}

package io.snabble.sdk.widgets.snabble.purchase.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebaslogen.resaca.viewModelScoped
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.PurchasesItem
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.dynamicview.theme.properties.padding
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.snabble.SnabbleCard
import io.snabble.sdk.widgets.snabble.purchase.OnLifecycleEvent
import io.snabble.sdk.widgets.snabble.purchase.Purchase
import io.snabble.sdk.widgets.snabble.purchase.viewmodel.Loading
import io.snabble.sdk.widgets.snabble.purchase.viewmodel.PurchaseViewModel
import io.snabble.sdk.widgets.snabble.purchase.viewmodel.ShowPurchases
import org.koin.core.component.get

@Composable
internal fun PurchaseWidget(
    model: PurchasesItem,
    viewModel: PurchaseViewModel = viewModelScoped { KoinProvider.get() },
    onAction: OnDynamicAction,
) {
    OnLifecycleEvent(Lifecycle.Event.ON_RESUME) { _, _ ->
        viewModel.updatePurchases()
    }

    val state = viewModel.state.collectAsStateWithLifecycle()

    when (val uiState = state.value) {
        Loading -> Unit
        is ShowPurchases -> {
            if (uiState.purchases.isNotEmpty()) {
                Purchases(
                    model = model,
                    purchaseList = uiState.purchases,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
fun Purchases(
    model: PurchasesItem,
    purchaseList: List<Purchase>,
    onAction: OnDynamicAction,
) {
    val isSinglePurchase: Boolean by remember(key1 = purchaseList.size == 1) { mutableStateOf(purchaseList.size == 1) }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = model.padding.bottom.dp)
    ) {
        val (title, more, purchases) = createRefs()
        val lastPurchasesTitleStringRes = if (isSinglePurchase) {
            R.string.Snabble_DynamicView_lastPurchase
        } else {
            R.string.Snabble_DynamicView_lastPurchases
        }
        Text(
            text = stringResource(id = lastPurchasesTitleStringRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(PaddingValues(horizontal = model.padding.start.dp + MaterialTheme.padding.small))
                .constrainAs(title) {
                    linkTo(start = parent.start, top = parent.top, end = more.start, bottom = purchases.top)
                    width = Dimension.fillToConstraints
                }
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .constrainAs(more) {
                    linkTo(start = title.end, top = title.top, end = parent.end, bottom = title.bottom)
                    height = Dimension.fillToConstraints
                }
                .padding(PaddingValues(horizontal = model.padding.start.dp + MaterialTheme.padding.small))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = false,
                        color = MaterialTheme.colorScheme.primary
                    ),
                ) {
                    onAction(DynamicAction(model, mapOf("action" to "more")))
                }
        ) {
            Text(
                text = stringResource(id = R.string.Snabble_DynamicView_LastPurchases_all),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.inversePrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier
                .padding(top = MaterialTheme.padding.large)
                .constrainAs(purchases) {
                    start.linkTo(parent.start)
                    top.linkTo(title.bottom)
                }
        ) {
            purchaseList.forEachIndexed { index, purchase ->
                PurchaseDetail(
                    modifier = Modifier
                        .weight(1f),
                    data = purchase,
                    clickAction = {
                        onAction(
                            DynamicAction(
                                widget = model,
                                info = mapOf(
                                    "action" to "purchase",
                                    "id" to purchase.id,
                                )
                            )
                        )
                    }
                )
                if (index < purchaseList.lastIndex) {
                    Spacer(modifier = Modifier.width(MaterialTheme.padding.large))
                }
            }
        }
    }
}

@Composable
private fun PurchaseDetail(
    modifier: Modifier = Modifier,
    data: Purchase,
    clickAction: () -> Unit,
) {
    SnabbleCard(
        modifier = modifier,
        onClick = clickAction
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(MaterialTheme.padding.medium))
        ) {
            val (
                icon,
                amount,
                title,
                time
            ) = createRefs()
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
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
                text = "${data.title}\n",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .constrainAs(title) {
                        top.linkTo(icon.bottom, margin = 12.dp)
                    }
            )
            Text(
                text = data.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
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

@Preview(backgroundColor = 0xEBEBEB, showBackground = true)
@Composable
private fun PurchaseDetailPreview() {
    ThemeWrapper {
        PurchaseDetail(
            data = Purchase("a01", "7,56 €", "Snabble Store Bonn Dransdorf", "Yesterday"),
            clickAction = {},
        )
    }
}

@Preview(backgroundColor = 0xEBEBEB, showBackground = true)
@Composable
private fun PurchaseWidgetPreview() {
    ThemeWrapper {
        Purchases(
            model = PurchasesItem(
                id = "last.purchases",
                padding = Padding(horizontal = 0)
            ),
            purchaseList = listOf(Purchase("a01", "13,37 €", "Snabble Store Bonn", "Today")),
            onAction = {},
        )
    }
}

@Preview(backgroundColor = 0xEBEBEB, showBackground = true)
@Composable
private fun TwoPurchasesPreview() {
    ThemeWrapper {
        Purchases(
            model = PurchasesItem(
                id = "last.purchases",
                padding = Padding(horizontal = 0)
            ),
            purchaseList = listOf(
                Purchase("a01", "13,37 €", "Snabble Store Bonn", "Today"),
                Purchase("a02", "7,56 €", "Snabble Store Bonn Dransdorf", "Yesterday"),
            ),
            onAction = {},
        )
    }
}

package io.snabble.sdk.ui.payment.payone.sepa.mandate.ui

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel.AcceptingMandateFailed
import io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel.Loading
import io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel.LoadingMandateFailed
import io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel.Mandate
import io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel.MandateAccepted
import io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel.UiState

@Composable
internal fun PayoneSepaMandateScreen(
    state: UiState,
    onAccepted: () -> Unit,
    onDenied: () -> Unit,
    onSuccessAction: () -> Unit,
    onErrorAction: () -> Unit,
) {
    when (state) {
        Loading -> LoadingScreen()

        is Mandate -> {
            MandateAcceptScreen(
                mandateHtml = state.mandateHtml,
                onAccepted = onAccepted,
                onDenied = onDenied
            )
        }

        AcceptingMandateFailed -> onErrorAction()

        LoadingMandateFailed -> onErrorAction()

        MandateAccepted -> onSuccessAction()
    }
}

@Composable
private fun LoadingScreen() {
    Row(
        modifier = Modifier
            .padding(all = 16.dp)
            .wrapContentSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.Snabble_pleaseWait),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MandateAcceptScreen(
    mandateHtml: String,
    onAccepted: () -> Unit,
    onDenied: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = {
                WebView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    webViewClient = WebViewClient()
                    loadData(mandateHtml, MIME_TYPE, ENCODING)
                }
            },
            update = {
                it.loadData(mandateHtml, MIME_TYPE, ENCODING)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            onClick = onAccepted,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colorScheme.primary,
            ),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                text = stringResource(id = R.string.Snabble_SEPA_iAgree),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.CenterHorizontally)
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable { onDenied() },
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(id = R.string.Snabble_SEPA_iDoNotAgree),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val MIME_TYPE = "text/html"
private const val ENCODING = "UTF-8"

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PayoneSepaMandateScreenPreview() {
    PayoneSepaMandateScreen(
        state = Mandate(mandateHtml = ""),
        onAccepted = {},
        onDenied = {},
        onSuccessAction = {},
        onErrorAction = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun LoadingScreenPreview() {
    LoadingScreen()
}

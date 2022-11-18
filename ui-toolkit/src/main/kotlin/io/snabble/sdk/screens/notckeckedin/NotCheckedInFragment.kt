package io.snabble.sdk.screens.notckeckedin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.ui.toolkit.R

class NotCheckedInFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                ThemeWrapper {
                    NotCheckedInScreen()
                }
            }
        }
}

@Composable
fun NotCheckedInScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally)
    {
        Image(
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 16.dp),
            painter = painterResource(id = R.drawable.snabble_offline_shop),
            colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.primary),
            contentDescription = "Store")
        Text(
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
            text = stringResource(id = R.string.Snabble_NotCheckedIn_message),
            textAlign = TextAlign.Center)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(16.dp, 0.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable {
                    SnabbleUiToolkit.executeAction(
                        context,
                        SnabbleUiToolkit.Event.SHOW_SHOP_LIST
                    )
                }
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(id = R.string.Snabble_DynamicView_Shop_show),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview
@Composable
fun Preview() {
    NotCheckedInScreen()
}

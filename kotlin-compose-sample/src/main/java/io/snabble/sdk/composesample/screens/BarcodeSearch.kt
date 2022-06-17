package io.snabble.sdk.composesample.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import io.snabble.sdk.composesample.databinding.BarcodeSearchBinding

object BarcodeSearch {
    val title = "Search"
    val route = "barcodeSearch"
}

@Composable
fun BarcodeSearch() {
    AndroidViewBinding(
        factory = BarcodeSearchBinding::inflate,
        modifier = Modifier.fillMaxSize(),
    )
}
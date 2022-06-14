package io.snabble.sdk.composesample.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.composesample.databinding.ScannerBinding
import androidx.compose.ui.viewinterop.AndroidViewBinding

@Composable
fun Scanner() {
    AndroidViewBinding(
        factory = ScannerBinding::inflate,
        modifier = Modifier.fillMaxSize(),
    )
}
package io.snabble.sdk.composesample.screens

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.composesample.databinding.ScannerBinding
import androidx.compose.ui.viewinterop.AndroidViewBinding
import io.snabble.sdk.ui.scanner.SelfScanningFragment

object Scanner {
    val title = "Scanner"
    val route = "scanner"
}

@Composable
fun Scanner() {
    AndroidViewBinding(
        factory = ScannerBinding::inflate,
        modifier = Modifier.fillMaxSize(),
    )
}
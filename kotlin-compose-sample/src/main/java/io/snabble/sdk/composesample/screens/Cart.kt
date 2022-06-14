package io.snabble.sdk.composesample.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import io.snabble.sdk.composesample.databinding.CartBinding

@Composable
fun Cart() {
    AndroidViewBinding(
        factory = CartBinding::inflate,
        modifier = Modifier.fillMaxSize(),
    )
}
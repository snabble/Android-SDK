package io.snabble.sdk.composesample.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidViewBinding
import io.snabble.sdk.composesample.databinding.CartBinding

object Cart {
    val title = "Cart"
    val route = "cart"
}

@Composable
fun Cart() {
    AndroidViewBinding(
        factory = CartBinding::inflate,
        modifier = Modifier.fillMaxSize(),
    )
}
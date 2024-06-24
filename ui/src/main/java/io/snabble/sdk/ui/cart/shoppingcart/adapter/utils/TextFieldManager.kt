package io.snabble.sdk.ui.cart.shoppingcart.adapter.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

class TextFieldManager(
    private val focusManager: FocusManager,
    private val keyboardController: SoftwareKeyboardController?,
) {

    fun clearFocusAndHideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
}

@Composable
fun rememberTextFieldManager(): TextFieldManager {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return TextFieldManager(focusManager, keyboardController)
}

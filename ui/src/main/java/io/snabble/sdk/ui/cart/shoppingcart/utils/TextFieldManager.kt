package io.snabble.sdk.ui.cart.shoppingcart.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

internal class TextFieldManager(
    private val focusManager: FocusManager,
    private val keyboardController: SoftwareKeyboardController?,
) {

    fun clearFocusAndHideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    
    fun showKeyboard(){
        keyboardController?.show()
    }

    fun moveFocusToNext() {
        focusManager.moveFocus(FocusDirection.Next)
    }
}

@Composable
internal fun rememberTextFieldManager(): TextFieldManager {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return TextFieldManager(focusManager, keyboardController)
}

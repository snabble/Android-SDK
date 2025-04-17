package io.snabble.sdk.ui

object ThemeManager {
    var primaryButtonConfig: PrimaryConfig = PrimaryConfig()
    var secondaryButtonConfig: SecondaryConfig = SecondaryConfig()
}


data class PrimaryConfig(
    val textSize: Int = 16,
    val minHeight: Int = 40,
)

data class SecondaryConfig(
    val textSize: Int = 16,
    val minHeight: Int = 40,
    val useOutlinedButton: Boolean = false
)

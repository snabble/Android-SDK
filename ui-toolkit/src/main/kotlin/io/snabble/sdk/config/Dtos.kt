package io.snabble.sdk.config

import io.snabble.sdk.widgets.ImageModel
import io.snabble.sdk.widgets.Widget

data class Config(
    val backgroundImage: ImageModel,
    val widgets: List<Widget>,
)



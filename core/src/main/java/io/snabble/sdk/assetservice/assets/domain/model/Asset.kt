package io.snabble.sdk.assetservice.assets.domain.model

import java.io.InputStream

internal data class Asset(
    val name: String,
    val hash: String,
    val data: InputStream,
)

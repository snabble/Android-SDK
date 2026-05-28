package io.snabble.sdk

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Class for describing our terms of service.
 */
data class TermsOfService(
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("version") val version: String,
    @SerializedName("variants") val variants: List<Variant> = emptyList()
) {
    /**
     * Gets a link to a downloadable html in the current system language or a default if no
     * translation is available.
     */
    val htmlLinkForSystemLanguage: String?
        get() {
            val defaultLocale = Locale.getDefault()
            variants.forEach {
                if (localeOf(it.language).language == defaultLocale.language) {
                    return Snabble.absoluteUrl(it.url)
                }
            }

            variants.forEach {
                if (it.isDefault) {
                    return Snabble.absoluteUrl(it.url)
                }
            }

            return if (variants.isNotEmpty()) {
                variants[0].let { Snabble.absoluteUrl(it.url) }
            } else null
        }

    /** A variant of the terms of service document */
    data class Variant(
        @SerializedName("isDefault") val isDefault: Boolean = false,
        @SerializedName("language") val language: String,
        @SerializedName("links") val links: Links
    ) {
        /** The relative url of the html document */
        val url: String
            get() = links.content.href
    }

    /** Class for link encapsulation */
    data class Links(
        @SerializedName("content") val content: Content
    )

    /** Class for link encapsulation */
    data class Content(
        @SerializedName("href") val href: String
    )
}

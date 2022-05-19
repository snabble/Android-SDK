package io.snabble.sdk

import java.util.*

/**
 * Class for describing our terms of service.
 */
data class TermsOfService(
    val updatedAt: String,
    val version: String,
    val variants: List<Variant> = emptyList()
) {
    /**
     * Gets a link to a downloadable html in the current system language or a default if no
     * translation is available.
     */
    val htmlLinkForSystemLanguage: String?
        get() {
            val defaultLocale = Locale.getDefault()
            variants.forEach {
                if (Locale(it.language).language == defaultLocale.language) {
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

    /**
     * A variant of the terms of service document
     */
    data class Variant(
        /**
         * True if this is the default terms of service to display if
         * a language specific one is not available
         */
        val isDefault: Boolean = false,
        /**
         * The iso2 language of the document
         */
        val language: String,
        /**
         * Link to the html document
         */
        val links: Links
    ) {
        /**
         * The relative url of the html document
         */
        val url: String
            get() = links.content.href
    }

    /**
     * Class for link encapsulation
     */
    data class Links(
        val content: Content
    )

    /**
     * Class for link encapsulation
     */
    data class Content(
        /**
         * The relative url of the html document
         */
        val href: String
    )
}
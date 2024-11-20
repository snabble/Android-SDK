package io.snabble.sdk

import com.google.gson.annotations.SerializedName

/**
 * Shop related on-site services
 */
data class ShopServices(
    /**
     * Url for the service related icon.
     *
     * The URL might be empty!
     */
    @SerializedName("iconURL") val iconPath: String,

    /**
     * Descriptions for the services in the language de and en.
     */
    @SerializedName("translations") val descriptions: Descriptions,
) {

    data class Descriptions(
        /**
         * Description in the language de.
         *
         * This value might be empty!
         */
        @SerializedName("de") val german: String?,

        /**
         * Description in the language en.
         *
         * This value might be empty!
         */
        @SerializedName("en") val english: String?
    )
}

package io.snabble.sdk.payment.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FormPrefillData(
    @SerializedName("name") val name: String? = null,
    @SerializedName("street") val street: String? = null,
    @SerializedName("zip") val zip: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("countryCode") val countryCode: String? = null,
    @SerializedName("stateCode") val stateCode: String? = null,
    @SerializedName("email") val email: String? = null
) : Parcelable

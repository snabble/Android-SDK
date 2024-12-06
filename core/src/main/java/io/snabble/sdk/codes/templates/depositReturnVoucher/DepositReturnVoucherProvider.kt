package io.snabble.sdk.codes.templates.depositReturnVoucher

import com.google.gson.JsonElement
import io.snabble.sdk.codes.templates.CodeTemplate

data class DepositReturnVoucherProvider(
    val id: String,
    val templates: List<CodeTemplate>
) {

    companion object {

        fun fromJsonElement(json: JsonElement): DepositReturnVoucherProvider? {
            val id = json.getValue("id")
            val templates = json.asJsonObject["templates"].asJsonArray
                ?.mapNotNull { it.getValue("template")?.let { template -> CodeTemplate(id, template) } }

            return when {
                id == null || templates == null -> null
                else -> DepositReturnVoucherProvider(id = id, templates = templates)
            }
        }
    }
}

private fun JsonElement.getValue(value: String): String? = asJsonObject[value].asString

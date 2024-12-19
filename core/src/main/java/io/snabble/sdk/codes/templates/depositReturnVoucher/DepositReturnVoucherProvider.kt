package io.snabble.sdk.codes.templates.depositReturnVoucher

import com.google.gson.JsonElement
import io.snabble.sdk.codes.templates.CodeTemplate
import io.snabble.sdk.utils.Logger

data class DepositReturnVoucherProvider(
    val id: String,
    val templates: List<CodeTemplate>
) {

    companion object {

        fun fromJsonElement(json: JsonElement): DepositReturnVoucherProvider? = try {
            val id: String? = json.getValue("id")
            val templates = json.asJsonObject["templates"].asJsonArray
                ?.mapNotNull { it.getValue("template") }
                ?.map { template -> CodeTemplate(id, template) }
            when {
                id == null || templates == null -> null
                else -> DepositReturnVoucherProvider(id = id, templates = templates)
            }
        } catch (e: IllegalStateException) {
            Logger.e("Couldn't parse deposit return provider: ${e.message}")
            null
        } catch (e: UnsupportedOperationException) {
            Logger.e("Couldn't parse deposit return provider: ${e.message}")
            null
        }
    }
}

private fun JsonElement.getValue(value: String): String? = asJsonObject[value].asString

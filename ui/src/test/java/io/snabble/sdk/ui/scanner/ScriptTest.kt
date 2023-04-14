package io.snabble.sdk.ui.scanner

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.snabble.sdk.ui.payment.data.CreditCardInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ScriptTest : FreeSpec({

    "Json to Creditcard" -{

        val format = Json { ignoreUnknownKeys = true }
       val data =  """
            {
                "hosteddataid": "hostedDataId",
                        "schemeTransactionId": "schemeTransactionId",
                        "cardnumber": "cardNumber",
                        "bname": "cardHolder",
                        "ccbrand": "ccBrand",
                        "expmonth": "expMonth",
                        "expyear": "expYear",
                        "processor_response_code": "responseCode",
                        "oid": "transactionId",
                        "fail_reason": "failReason"
            }
        """.trimIndent()

        val credit = format.decodeFromString<CreditCardInfo>(data)
        credit.expirationYear.shouldBe("expYear")
        println(credit)
    }
})

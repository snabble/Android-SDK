package io.snabble.sdk.ui.payment.creditcard.data

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.snabble.sdk.ui.payment.creditcard.data.CreditCardInfo.Companion.toCreditCardInfo

class CreditCardInfoTest : FreeSpec({

    "A credit card parsed from a Json should return the matching credit card info" - {
        val data = """
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

        val credit = data.toCreditCardInfo()
        credit.expirationYear.shouldBe("expYear")
    }
})

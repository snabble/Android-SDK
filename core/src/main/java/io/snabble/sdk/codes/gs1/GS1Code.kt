package io.snabble.sdk.codes.gs1

import kotlin.math.max
import kotlin.math.min

class GS1Code(val gs1Code: String) {
    companion object {
        const val GS = "\u001D"

        val symbologyIdentifiers = listOf(
                "]C1",  // = GS1-128
                "]e0",  // = GS1 DataBar
                "]d2",  // = GS1 DataMatrix
                "]Q3",  // = GS1 QR Code
                "]J1"   // = GS1 DotCode
        )
    }

    val identifiers: ArrayList<Element>
    val skipped: ArrayList<String>

    private var code: String

    init {
        identifiers = ArrayList()
        skipped = ArrayList()
        code = gs1Code

        parse()
    }

    private fun parse() {
        symbologyIdentifiers.forEach {
            code = code.removePrefix(it)
        }

        while (nextElement()){}
    }

    private fun nextElement(): Boolean {
        while (code.startsWith(GS) && code.isNotEmpty()) {
            code = code.removePrefix(GS)
        }

        if (code.length >= 2) {
            val prefix = code.substring(0, 2)
            val elementLength = ApplicationIdentifier.elementLength(prefix)
            val elementString = if (elementLength > 0) {
                code.substring(0, min(code.length, elementLength))
            } else {
                code.substringBefore(GS)
            }

            if (elementString.isNotEmpty()) {
                ApplicationIdentifier.byPrefix(prefix)?.forEach { ai ->
                    val remaining = code.removePrefix(prefix)
                    if (remaining.startsWith(ai.additionalIdentifier ?: "")) {
                        val contentLength = ai.contentLength

                        code = if (contentLength > 0) {
                            code.substringAfter(elementString, "")
                        } else {
                            code.substringAfter(GS, "")
                        }

                        val result = Regex(ai.regex).find(elementString)
                        if (result != null) {
                            identifiers.add(Element(ai, result.groupValues.drop(1)))
                            val leftover = elementString.removeRange(result.range)
                            if (leftover.isNotEmpty()) {
                                skipped.add(leftover)
                            }
                        } else {
                            skipped.add(elementString)
                        }
                        return true
                    }
                }

                skipped.add(elementString)
                code = code.substringAfter(elementString, "")
                return true
            } else {
                return true
            }
        }

        return false
    }
}
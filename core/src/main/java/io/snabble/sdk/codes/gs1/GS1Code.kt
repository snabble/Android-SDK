package io.snabble.sdk.codes.gs1

class GS1Code(val code: String) {
    companion object {
        const val GS = "\u001D"
    }

    val identifiers: ArrayList<Element>
    val skipped: ArrayList<String>

    init {
        identifiers = ArrayList()
        skipped = ArrayList()

        parse()
    }

    private fun parse() {
        var code = this.code
        symbologyIdentifiers.forEach {
            code = code.removePrefix(it)
        }

        while (code.isNotEmpty()) {
            while (code.startsWith(GS)) {
                code = code.removePrefix(GS)
            }

            val prefix = code.substring(0, 2)
            ApplicationIdentifier.byPrefix(prefix)?.let { list ->
                list.forEach { ai ->
                    val remaining = ai.prefix.removePrefix(prefix)
                    if (remaining == ai.additionalIdentifier) {
                        val contentLength = ai.contentLength
                        if (contentLength == 0) {
                            val content = code.substringBefore(GS, "")
                            if (content.isNotEmpty()) {
                                identifiers.add(Element(ai, content))
                            }
                        }
                    }
                }
            }
        }
    }

    val symbologyIdentifiers = listOf(
        "]C1",  // = GS1-128
        "]e0",  // = GS1 DataBar
        "]d2",  // = GS1 DataMatrix
        "]Q3",  // = GS1 QR Code
        "]J1"   // = GS1 DotCode
    )
}
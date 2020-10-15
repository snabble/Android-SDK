package io.snabble.sdk.codes.gs1

class GS1Code(val code: String) {



    val identifiers: List<Element>
    val skipped: List<String>

    init {
        identifiers = ArrayList()
        skipped = ArrayList()
    }

    private fun parse() {

    }
}
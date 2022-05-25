package io.snabble.sdk.codes

import io.snabble.sdk.Project
import io.snabble.sdk.Unit
import java.io.Serializable
import java.math.BigDecimal
import java.util.ArrayList

// TODO: Kotlinify this class more, get rid of the "hasXX" methods and use optionals

/**
 * Class representing a scanned code, including its potentially embedded data
 */
class ScannedCode private constructor() : Serializable {
    private var _embeddedData: Int? = null
    private var _embeddedDecimalData: BigDecimal? = null
    private var _price: Int? = null

    /**
     * The code that you use to lookup the product in the product database
     */
    var lookupCode: String? = null
        private set

    /**
     * The code that was actually scanned
     */
    var code: String? = null
        private set

    /**
     * The template that this code matches to
     */
    var templateName: String? = null
        private set

    /**
     * The template that this code may be transformed to when embedding in encoded codes
     */
    var transformationTemplateName: String? = null
        private set

    /**
     * The code used when embedding in encoded codes
     */
    var transformationCode: String? = null
        private set

    var embeddedData: Int
        /**
         * Get the data embedded in the code, or 0
         */
        get() = _embeddedData?.let { it } ?: 0
        /**
         * Set the data embedded in the code, or 0
         */
        set(value) {
            _embeddedData = value
        }

    /**
     * Returns true if the code holds embedded data
     */
    fun hasEmbeddedData(): Boolean {
        return _embeddedData != null
    }

    var embeddedDecimalData: BigDecimal
        /**
         * Get the data embedded in the code, as a big decimal
         */
        get() = _embeddedDecimalData?.let { it } ?: BigDecimal.ZERO
        /**
         * Set the data embedded in the code, as a big decimal
         */
        set(value) {
            _embeddedDecimalData = value
        }

    /**
     * Returns true if the code holds embedded decimal data
     */
    fun hasEmbeddedDecimalData(): Boolean {
        return _embeddedDecimalData != null
    }

    /**
     * Gets the unit of the embedded data
     */
    var embeddedUnit: Unit? = null

    /**
     * Gets the embedded price
     */
    val price: Int
        get() = _price?.let { it } ?: 0

    /**
     * Returns true if the code holds a embedded price
     */
    fun hasPrice(): Boolean {
        return _price != null
    }

    /**
     * Creates a new builder, based on this code.
     */
    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Builder {
        var scannedCode: ScannedCode

        constructor(templateName: String?) {
            scannedCode = ScannedCode().apply {
                this.templateName = templateName
            }
        }

        constructor(source: ScannedCode) {
            scannedCode = ScannedCode().apply {
                _embeddedData = source._embeddedData
                _embeddedDecimalData = source._embeddedDecimalData
                _price = source._price
                lookupCode = source.lookupCode
                code = source.code
                templateName = source.templateName
                embeddedUnit = source.embeddedUnit
                transformationTemplateName = source.transformationTemplateName
                transformationCode = source.transformationCode
            }
        }

        fun setScannedCode(scannedCode: String?) = apply {
            this.scannedCode.code = scannedCode
        }

        fun setLookupCode(lookupCode: String?) = apply {
            this.scannedCode.lookupCode = lookupCode
        }

        fun setEmbeddedData(embeddedData: Int) = apply {
            this.scannedCode._embeddedData = embeddedData
        }

        fun setEmbeddedDecimalData(embeddedDecimalData: BigDecimal?) = apply {
            this.scannedCode._embeddedDecimalData = embeddedDecimalData
        }

        fun setEmbeddedUnit(unit: Unit?) = apply {
            this.scannedCode.embeddedUnit = unit
        }

        fun setTransformationCode(transformationCode: String?) = apply {
            this.scannedCode.transformationCode = transformationCode
        }

        fun setTransformationTemplateName(name: String?) = apply {
            this.scannedCode.transformationTemplateName = name
        }

        fun setPrice(price: Int) = apply {
            this.scannedCode._price = price
        }

        fun create(): ScannedCode {
            return scannedCode
        }
    }

    companion object {
        @JvmStatic
        fun parseDefault(project: Project, code: String?): ScannedCode? {
            project.codeTemplates.forEach { codeTemplate ->
                val scannedCode = codeTemplate.match(code).buildCode()
                if (scannedCode != null && scannedCode.templateName == "default") {
                    return scannedCode
                }
            }
            return null
        }

        @JvmStatic
        fun parse(project: Project, code: String): List<ScannedCode> {
            val matches: MutableList<ScannedCode> = ArrayList()
            val defaultTemplate = project.defaultCodeTemplate

            project.codeTemplates.forEach { codeTemplate ->
                val scannedCode = codeTemplate.match(code).buildCode()
                if (scannedCode != null) {
                    matches.add(scannedCode)
                }
            }

            project.priceOverrideTemplates.forEach { priceOverrideTemplate ->
                val codeTemplate = priceOverrideTemplate.codeTemplate
                val scannedCode = codeTemplate.match(code).buildCode()
                scannedCode?.let {
                    val lookupCode = scannedCode.lookupCode
                    if (lookupCode != code) {
                        val defaultCode = defaultTemplate?.match(lookupCode)?.buildCode()
                        defaultCode?.let {
                            defaultCode._embeddedData = scannedCode._embeddedData
                            defaultCode.embeddedUnit = Unit.PRICE
                            defaultCode.code = scannedCode.code
                            val transformTemplate = priceOverrideTemplate.transmissionCodeTemplate
                            if (transformTemplate != null) {
                                defaultCode.transformationTemplateName = transformTemplate.name
                                defaultCode.transformationCode = priceOverrideTemplate.transmissionCode
                            }
                            matches.add(defaultCode)
                        }
                    }
                }
            }
            return matches
        }
    }
}
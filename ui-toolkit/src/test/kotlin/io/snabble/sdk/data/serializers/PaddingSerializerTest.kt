package io.snabble.sdk.data.serializers

import com.google.gson.JsonParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.snabble.sdk.data.PaddingDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class PaddingSerializerTest : FreeSpec({

    "Decoding a list" - {

        "successfully w/ correct value amount" - {

            "of 4 padding values" {
                val paddingsJson = "[1,2,3,4]"
                val sut: PaddingDto = Json.decodeFromString(paddingsJson)

                sut shouldBe PaddingDto(start = 1, top = 2, end = 4, bottom = 3)
            }

            "of 2 padding values" {
                val paddingsJson = "[1,2]"
                val sut: PaddingDto = Json.decodeFromString(paddingsJson)

                sut shouldBe PaddingDto(start = 1, top = 2, end = 1, bottom = 2)
            }

            "of 1 padding values" {
                val paddingsJson = "[1]"
                val sut: PaddingDto = Json.decodeFromString(paddingsJson)

                sut shouldBe PaddingDto(start = 1, top = 1, end = 1, bottom = 1)
            }
        }

        "unsuccessfully w/ incorrect value amount" - {

            "of 3 padding values" {
                val paddingsJson = "[1,2,3]"

                shouldThrow<JsonParseException> {
                    Json.decodeFromString<PaddingDto>(paddingsJson)
                }
            }

            "of 5 padding values" {
                val paddingsJson = "[1,2,3,4,5]"

                shouldThrow<JsonParseException> {
                    Json.decodeFromString<PaddingDto>(paddingsJson)
                }
            }

            "of more than 5 padding values" {
                val paddingsJson = "[1,2,3,4,5,6,7,8,9,0]"

                shouldThrow<JsonParseException> {
                    Json.decodeFromString<PaddingDto>(paddingsJson)
                }
            }
        }
    }

    "Encoding Padding values to a JSON array of numbers" {
        val padding = PaddingDto(start = 1, top = 2, end = 4, bottom = 3)
        val sut = Json.encodeToString(padding)

        sut shouldBe """[1,2,3,4]"""
    }
})

package io.snabble.sdk.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.Widget
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Text
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SampleTest : FreeSpec({

    "a" - {

        "b" {
            val text = Text(
                id = 5, text = "Hello World!", spacing = 5
            )
            val json = Json {
                encodeDefaults = true
                classDiscriminator = "type"
            }
            val jsonString = json.encodeToString(text)
            println("Message: $jsonString")
            println("Object: ${json.decodeFromString<Text>(jsonString)}")
        }

        "c" {
            val image = ImageDto(
                id = 6,
                imageSource = "R.drawable.background",
                spacing = 5
            )
            val json = Json {
                encodeDefaults = true
            }
            val jsonString = json.encodeToString(image)
            println(jsonString)
            println("${json.decodeFromString<Image>(jsonString)}")
        }

        "d" {
            val text = TextDto(
                id = 5, text = "Hello World!", spacing = 5
            )
            val image = ImageDto(
                id = 6,
                imageSource = "R.drawable.background",
                spacing = 5
            )
            "Ulla.json"
            val json = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
            // val jsonString = json.encodeToString(listOf(text, image))
            val jsonString =
                "[{\"type\":\"text\",\"id\":5,\"text\":\"Hello World!\",\"textColorSource\":null,\"textStyleSource\":null,\"showDisclosure\":null,\"spacing\":null},{\"type\":\"image\",\"id\":6,\"imageSource\":\"R.drawable.background\",\"spacing\":5.0},{\"type\":\"kuchen\",\"id\":6,\"imageSource\":\"R.drawable.background\",\"spacing\":5.0}]\n"
            println(jsonString)
            shouldThrow<SerializationException> {
                json.decodeFromString<List<Widget>>(jsonString)
            }
        }
    }
})

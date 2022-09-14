package io.snabble.sdk.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.snabble.sdk.widgets.ImageModel
import io.snabble.sdk.widgets.TextModel
import io.snabble.sdk.widgets.Widget
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SampleTest : FreeSpec({

    "a" - {

        "b" {
            val text = TextModel(
                id = 5, text = "Hello World!",
            )
            val json = Json {
                encodeDefaults = true
                classDiscriminator = "type"
            }
            val jsonString = json.encodeToString(text)
            println("Message: $jsonString")
            println("Object: ${json.decodeFromString<TextModel>(jsonString)}")
        }

        "c" {
            val image = ImageModel(
                id = 6,
                imageSource = "R.drawable.background",
                spacing = 5f
            )
            val json = Json {
                encodeDefaults = true
            }
            val jsonString = json.encodeToString(image)
            println(jsonString)
            println("${json.decodeFromString<ImageModel>(jsonString)}")
        }

        "d" {
            val text = TextModel(
                id = 5, text = "Hello World!",
            )
            val image = ImageModel(
                id = 6,
                imageSource = "R.drawable.background",
                spacing = 5f
            )
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

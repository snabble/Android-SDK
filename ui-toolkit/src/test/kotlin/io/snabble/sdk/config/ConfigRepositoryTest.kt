package io.snabble.sdk.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class ConfigRepositoryTest : FreeSpec({

    fun createSut(json: String) = ConfigRepository(
        fileProvider = mockk { coEvery { getFile(any()) } returns json },
        json = Json
    )

    "Parsing a " - {

        "well formed json string containing" - {

            "a simple object" {
                val simpleJsonObject = """{ "name": "John", "age": 32 }"""

                val sut = createSut(simpleJsonObject)
                val person: Person = sut.getConfig("")

                person.name shouldBe "John"
                person.age shouldBe 32
            }

            "a polymorphic array" {
                val polymorphicJsonObject =
                    """
                    [
                      { "type": "a.user", "name": "John" , "age": 32 },
                      { "type": "a.thing", "label": "Socket", "purpose": "power delivery" }
                    ]
                """.trimIndent()

                val sut = createSut(polymorphicJsonObject)
                val world: List<World> = sut.getConfig("")

                world.size shouldBe 2
                world.shouldContainInOrder(
                    Person(name = "John", age = 32),
                    Thing(label = "Socket", purpose = "power delivery")
                )
            }
        }

        "not well formed json will throw an exception" {
            val simpleJsonObject = """ {"name" = "John", "age": 32 }""" // = should be :

            val sut = createSut(simpleJsonObject)

            shouldThrow<SerializationException> {
                sut.getConfig<Person>("users.json")
            }
        }
    }
})

@Serializable
private sealed interface World

@Serializable
@SerialName("a.user")
private data class Person(val name: String, val age: Int) : World

@Serializable
@SerialName("a.thing")
private data class Thing(val label: String, val purpose: String) : World

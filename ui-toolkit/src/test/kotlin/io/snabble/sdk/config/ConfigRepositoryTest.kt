package io.snabble.sdk.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ConfigRepositoryTest : FreeSpec({

    fun createSut(json: String) = ConfigRepository(
        fileProvider = mockk { coEvery { getFile(any()) } returns json },
        json = Json
    )

    "Parsing a " - {

        "well formed json string" - {

            "w/ a simple object" {
                val simpleJsonObject = """{ "name": "John", "age": 32 }"""

                val sut = createSut(simpleJsonObject)
                val person: Person = sut.getConfig("")

                person.name shouldBe "John"
                person.age shouldBe 32
            }

            "w/ a polymorphic array" {
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
                world.shouldContainAll(
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
sealed interface World

@Serializable
@SerialName("a.user")
data class Person(val name: String, val age: Int) : World

@Serializable
@SerialName("a.thing")
data class Thing(val label: String, val purpose: String) : World

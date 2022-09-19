package io.snabble.sdk.mapper

import android.content.Context
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.snabble.sdk.data.ConfigurationDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.domain.ConfigMapperImpl
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Text
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId

internal class ConfigMapperTest : FreeSpec({

    val context = mockk<Context>(relaxed = true)
    fun createMapper() = ConfigMapperImpl(
        context
    )

    beforeEach {
        clearAllMocks()
    }

    "Mapping a rootDto to root with" - {

        "rootDto containing a config and an empty Widget list" - {

            val rootDto = RootDto(
                ConfigurationDto(image = "R.drawable.abc", style = "", padding = 0),
                emptyList()
            )
            every { context.resolveImageId(rootDto.configuration.image) } returns 5

            val sut = createMapper().mapTo(rootDto)

            "the root config is successfully mapped" - {

                "image"{
                    sut.configuration.image shouldBe 5
                }
                "padding"{
                    sut.configuration.padding shouldBe 0
                }
                "style"{
                    sut.configuration.style shouldBe ""
                }
            }

            "the root Widget list is successfully mapped" {
                sut.widgets shouldBe emptyList()
            }
        }

        "rootDto containing a config and a List of Widgets" - {
            val imageDto = ImageDto("an.image", "R.drawable.abc", 5)
            val textDto = TextDto("a.title", "Hello World", "asdb", null, null, 5)

            val rootDto = RootDto(
                ConfigurationDto(image = "R.drawable.abc", style = "", padding = 0),
                listOf(imageDto, textDto)
            )
            every { context.resolveImageId(rootDto.configuration.image) } returns 5
            every { context.resolveImageId(imageDto.imageSource) } returns 5
            every { context.resolveColorId(textDto.textColorSource) } returns 8

            val sut = createMapper().mapTo(rootDto)

            "the root Widget list is successfully mapped" - {

                "Image Type" - {

                    val image = sut.widgets.first().shouldBeTypeOf<Image>()

                    "Image properties"{
                        image.imageSource shouldBe 5
                        image.id shouldBe "an.image"
                    }
                }

                "Text Type" - {

                    val text = sut.widgets[1].shouldBeTypeOf<Text>()

                    "Text poperties"{
                        text.textColorSource shouldBe 8
                        text.id shouldBe "a.title"
                    }
                }
            }
        }
    }
})

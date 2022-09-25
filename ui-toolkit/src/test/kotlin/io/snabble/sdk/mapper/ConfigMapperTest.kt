package io.snabble.sdk.mapper

import android.content.Context
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.snabble.sdk.data.ConfigurationDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.PaddingDto
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.domain.ConfigMapperImpl
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.resolveImageId

internal class ConfigMapperTest : FreeSpec({

    val context = mockk<Context>(relaxed = true)
    fun createMapper() = ConfigMapperImpl(context)

    beforeEach {
        clearAllMocks()
    }

    "Mapping a rootDto to root with" - {

        "rootDto containing a config and an empty Widget list" - {

            val rootDto = RootDto(
                ConfigurationDto(
                    image = "R.drawable.abc",
                    style = "",
                    padding = PaddingDto(0, 0, 0, 0)
                ),
                emptyList()
            )
            every { context.resolveImageId(rootDto.configuration.image) } returns 5

            val sut = createMapper().mapTo(rootDto)

            "the root config is successfully mapped" - {

                "image"{
                    sut.configuration.image shouldBe 5
                }
                "padding"{
                    sut.configuration.padding shouldBe Padding(0, 0, 0, 0)
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
            mockkStatic("io.snabble.sdk.utils.KotlinExtensions")

            val imageDto = ImageDto("an.image", "R.drawable.abc", PaddingDto(0, 0, 0, 0))
            val textDto = TextDto(
                "a.title",
                "Hello World",
                "asdb",
                null,
                null,
                PaddingDto(0, 0, 0, 0)
            )

            val rootDto = RootDto(
                ConfigurationDto(
                    image = "R.drawable.abc",
                    style = "",
                    padding = PaddingDto(0, 0, 0, 0)
                ),
                listOf(imageDto, textDto)
            )
            every { context.resolveImageId(rootDto.configuration.image) } returns 5
            every { context.resolveImageId(imageDto.imageSource) } returns 5
            every { context.getComposeColor(textDto.textColorSource) } returns 8

            val sut = createMapper().mapTo(rootDto)

            "the root Widget list is successfully mapped" - {

                "Image Type" - {

                    val imageItem = sut.widgets.first().shouldBeTypeOf<ImageItem>()

                    "Image properties"{
                        imageItem.imageSource shouldBe 5
                        imageItem.id shouldBe "an.image"
                    }
                }

                "Text Type" - {

                    val textItem = sut.widgets[1].shouldBeTypeOf<TextItem>()

                    "Text poperties"{
                        textItem.textColorSource shouldBe 8
                        textItem.id shouldBe "a.title"
                    }
                }
            }
        }
    }
})

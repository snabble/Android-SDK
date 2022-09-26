package io.snabble.sdk.mapper

import android.content.Context
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.snabble.sdk.data.ButtonDto
import io.snabble.sdk.data.ConfigurationDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.PaddingDto
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.WidgetDto
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.ConfigMapperImpl
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.resolveImageId

internal class ConfigMapperTest : FreeSpec({

    val context = mockk<Context>(relaxed = true)
    fun createMapper() = ConfigMapperImpl(context)
    fun setUpSutDto(widgetDto: WidgetDto?): RootDto {
        if (widgetDto == null) {
            return RootDto(
                ConfigurationDto(
                    image = "R.drawable.abc",
                    style = "",
                    padding = PaddingDto(0, 0, 0, 0)
                ),
                emptyList()
            )
        } else {
            return RootDto(
                ConfigurationDto(
                    image = "R.drawable.abc",
                    style = "",
                    padding = PaddingDto(0, 0, 0, 0)
                ),
                listOf(widgetDto)
            )
        }
    }

    beforeEach {
        clearAllMocks()
    }

    "Mapping a" - {

        "rootDto containing a config and an empty Widget list" - {

            val rootDto = setUpSutDto(null)

            every { context.resolveImageId(rootDto.configuration.image) } returns 5

            val sut = createMapper().mapTo(rootDto)

            "configurationDto to ConfigurationItem" - {

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

            "widgetDto list to empty widgetItem list" {
                sut.widgets shouldBe emptyList()
            }
        }

        "rootDto containing a config and a List of Widgets" - {
            mockkStatic("io.snabble.sdk.utils.KotlinExtensions")

            "ImageDto to ImageItem" - {

                val imageDto = ImageDto(
                    id = "an.image",
                    imageSource = "R.drawable.abc",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                every { context.resolveImageId(imageDto.imageSource) } returns 5

                val rootDto = setUpSutDto(imageDto)
                val sut = createMapper().mapTo(rootDto)

                val imageItem = sut.widgets.first().shouldBeTypeOf<ImageItem>()

                "id" {
                    imageItem.id shouldBe "an.image"
                }
                "image" {
                    imageItem.imageSource shouldBe 5
                }
                "padding"{
                    imageItem.padding shouldBe Padding(0, 0, 0, 0)
                }

            }

            "TextDto to TextItem" - {

                val textDto = TextDto(
                    id = "a.title",
                    text = "Hello World",
                    textColorSource = "asd",
                    textStyleSource = null,
                    showDisclosure = null,
                    PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(textDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 5
                every { context.getComposeColor(textDto.textColorSource) } returns 8
                val sut = createMapper().mapTo(rootDto)

                val textItem = sut.widgets.first().shouldBeTypeOf<TextItem>()


                "id" {
                    textItem.id shouldBe "a.title"
                }
                "text"{
                    textItem.text shouldBe "Hello World"
                }
                "text color"{
                    textItem.textColorSource shouldBe 8
                }
                "text style"{
                    textItem.textStyleSource shouldBe null
                }
                "padding"{
                    textItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "ButtonDto to ButtonItem" - {

                val buttonDto = ButtonDto(
                    id = "a.button",
                    text = "Hello World",
                    foregroundColorSource = "test",
                    backgroundColorSource = null,
                    padding = PaddingDto(0,0,0,0)
                )

                val rootDto = setUpSutDto(buttonDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1
                every { context.getComposeColor(buttonDto.foregroundColorSource) } returns 2
                every { context.getComposeColor(buttonDto.backgroundColorSource) } returns 3
                val sut = createMapper().mapTo(rootDto)

                val buttonItem = sut.widgets.first().shouldBeTypeOf<ButtonItem>()


                "id" {
                    buttonItem.id shouldBe "a.button"
                }
                "text"{
                    buttonItem.text shouldBe "Hello World"
                }
                "foreground color"{
                    buttonItem.foregroundColorSource shouldBe 2
                }
                "background color"{
                    buttonItem.backgroundColorSource shouldBe 3
                }
                "padding"{
                    buttonItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }
        }
    }
})

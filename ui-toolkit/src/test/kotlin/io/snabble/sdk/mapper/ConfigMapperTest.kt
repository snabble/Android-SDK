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
import io.snabble.sdk.data.ConnectWifiDto
import io.snabble.sdk.data.CustomerCardDto
import io.snabble.sdk.data.DynamicConfigDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.InformationDto
import io.snabble.sdk.data.LocationPermissionDto
import io.snabble.sdk.data.PaddingDto
import io.snabble.sdk.data.SectionDto
import io.snabble.sdk.data.SeeAllStoresDto
import io.snabble.sdk.data.StartShoppingDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.WidgetDto
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.ConfigMapperImpl
import io.snabble.sdk.domain.ConnectWifiItem
import io.snabble.sdk.domain.CustomerCardItem
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.SectionItem
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.getResourceString
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId

internal class ConfigMapperTest : FreeSpec({

    val context = mockk<Context>(relaxed = true)
    fun createMapper() = ConfigMapperImpl(context)
    fun setUpSutDto(widgetDto: WidgetDto?): DynamicConfigDto {
        if (widgetDto == null) {
            return DynamicConfigDto(
                ConfigurationDto(
                    image = "R.drawable.abc",
                    style = "",
                    padding = PaddingDto(0, 0, 0, 0)
                ),
                emptyList()
            )
        } else {
            return DynamicConfigDto(
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

        "rootDto containing a config and an empty widget list" - {

            val rootDto = setUpSutDto(null)

            every { context.resolveImageId(rootDto.configuration.image) } returns 1

            val sut = createMapper().mapTo(rootDto)

            "configurationDto to ConfigurationItem" - {

                "image"{
                    sut.configuration.image shouldBe 1
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

        "rootDto containing a config and a list of widgets" - {
            mockkStatic("io.snabble.sdk.utils.KotlinExtensions")

            "ImageDto to ImageItem" - {

                val imageDto = ImageDto(
                    id = "an.image",
                    imageSource = "R.drawable.abc",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                every { context.resolveImageId(imageDto.imageSource) } returns 1

                val rootDto = setUpSutDto(imageDto)
                val sut = createMapper().mapTo(rootDto)

                val imageItem = sut.widgets.first().shouldBeTypeOf<ImageItem>()

                "id" {
                    imageItem.id shouldBe "an.image"
                }
                "image" {
                    imageItem.imageSource shouldBe 1
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
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(buttonDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1
                every { context.getResourceString(buttonDto.text) } returns "Hello World"
                every { context.resolveColorId(buttonDto.foregroundColorSource) } returns 2
                every { context.resolveColorId(buttonDto.backgroundColorSource) } returns 3

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

            "LocationPermissionDto to LocationPermissionItem" - {

                val locationPermissionDto = LocationPermissionDto(
                    id = "a.location",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(locationPermissionDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapTo(rootDto)

                val locationPermissionItem = sut.widgets.first().shouldBeTypeOf<LocationPermissionItem>()

                "id" {
                    locationPermissionItem.id shouldBe "a.location"
                }
                "padding"{
                    locationPermissionItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "SeeAllStoresDto to SeeAllStoresItem" - {

                val seeAllStoreDto = SeeAllStoresDto(
                    id = "a.store",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(seeAllStoreDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapTo(rootDto)

                val seeAllStoresItem = sut.widgets.first().shouldBeTypeOf<SeeAllStoresItem>()

                "id" {
                    seeAllStoresItem.id shouldBe "a.store"
                }
                "padding"{
                    seeAllStoresItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "StartShoppingDto to StartShoppingItem" - {

                val startShoppingDto = StartShoppingDto(
                    id = "a.start",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(startShoppingDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapTo(rootDto)

                val startShoppingItem = sut.widgets.first().shouldBeTypeOf<StartShoppingItem>()

                "id" {
                    startShoppingItem.id shouldBe "a.start"
                }
                "padding"{
                    startShoppingItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "InformationDto to InformationItem" - {

                val informationDto = InformationDto(
                    id = "a.info",
                    text = "information",
                    imageSource = null,
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(informationDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1
                every { context.resolveImageId(informationDto.imageSource) } returns 2

                val sut = createMapper().mapTo(rootDto)

                val informationItem = sut.widgets.first().shouldBeTypeOf<InformationItem>()

                "id" {
                    informationItem.id shouldBe "a.info"
                }
                "text" {
                    informationItem.text shouldBe "information"
                }
                "image" {
                    informationItem.imageSource shouldBe 2
                }
                "padding"{
                    informationItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "CustomerCardDto to CustomerCardItem" - {

                val customerCardDto = CustomerCardDto(
                    id = "a.card",
                    text = "customerCard",
                    imageSource = null,
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(customerCardDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1
                every { context.resolveImageId(customerCardDto.imageSource) } returns 2

                val sut = createMapper().mapTo(rootDto)

                val customerCardItem = sut.widgets.first().shouldBeTypeOf<CustomerCardItem>()

                "id" {
                    customerCardItem.id shouldBe "a.card"
                }
                "text" {
                    customerCardItem.text shouldBe "customerCard"
                }
                "image" {
                    customerCardItem.imageSource shouldBe 2
                }
                "padding"{
                    customerCardItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "ConnectWifiDto to ConnectWifiItem" - {

                val connectWifiDto = ConnectWifiDto(
                    id = "a.wifi",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(connectWifiDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapTo(rootDto)

                val connectWifiItem = sut.widgets.first().shouldBeTypeOf<ConnectWifiItem>()

                "id" {
                    connectWifiItem.id shouldBe "a.wifi"
                }
                "padding"{
                    connectWifiItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "SectionDto to SectionItem" - {

                val sectionDto = SectionDto(
                    id = "a.section",
                    header = "test",
                    widgets = listOf(
                        ConnectWifiDto(
                            id = "a.wifi",
                            padding = PaddingDto(0, 0, 0, 0)
                        )
                    ),
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setUpSutDto(sectionDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapTo(rootDto)

                val sectionItem = sut.widgets.first().shouldBeTypeOf<SectionItem>()

                "id" {
                    sectionItem.id shouldBe "a.section"
                }
                "header" {
                    sectionItem.header shouldBe "test"
                }
                "items" {
                    sectionItem.items.first().shouldBeTypeOf<ConnectWifiItem>()
                }
                "padding"{
                    sectionItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

        }
    }
})

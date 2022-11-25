package io.snabble.sdk.dynamicview.data.dto.mapper

import android.content.Context
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.snabble.sdk.dynamicview.data.dto.AppUserIdDto
import io.snabble.sdk.dynamicview.data.dto.ButtonDto
import io.snabble.sdk.dynamicview.data.dto.ClientIdDto
import io.snabble.sdk.dynamicview.data.dto.ConfigurationDto
import io.snabble.sdk.dynamicview.data.dto.ConnectWlanDto
import io.snabble.sdk.dynamicview.data.dto.CustomerCardDto
import io.snabble.sdk.dynamicview.data.dto.DevSettingsDto
import io.snabble.sdk.dynamicview.data.dto.DynamicConfigDto
import io.snabble.sdk.dynamicview.data.dto.ImageDto
import io.snabble.sdk.dynamicview.data.dto.InformationDto
import io.snabble.sdk.dynamicview.data.dto.LocationPermissionDto
import io.snabble.sdk.dynamicview.data.dto.PaddingDto
import io.snabble.sdk.dynamicview.data.dto.SectionDto
import io.snabble.sdk.dynamicview.data.dto.SeeAllStoresDto
import io.snabble.sdk.dynamicview.data.dto.StartShoppingDto
import io.snabble.sdk.dynamicview.data.dto.TextDto
import io.snabble.sdk.dynamicview.data.dto.VersionDto
import io.snabble.sdk.dynamicview.data.dto.WidgetDto
import io.snabble.sdk.dynamicview.domain.model.AppUserIdItem
import io.snabble.sdk.dynamicview.domain.model.ButtonItem
import io.snabble.sdk.dynamicview.domain.model.ClientIdItem
import io.snabble.sdk.dynamicview.domain.model.ConnectWlanItem
import io.snabble.sdk.dynamicview.domain.model.CustomerCardItem
import io.snabble.sdk.dynamicview.domain.model.DevSettingsItem
import io.snabble.sdk.dynamicview.domain.model.ImageItem
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.SeeAllStoresItem
import io.snabble.sdk.dynamicview.domain.model.StartShoppingItem
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.VersionItem
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.utils.getResourceString
import io.snabble.sdk.utils.resolveColorId
import io.snabble.sdk.utils.resolveImageId

internal class ConfigMapperImplTest : FreeSpec({

    val context = mockk<Context>(relaxed = true)

    fun createMapper() = ConfigMapperImpl(context, ssidProvider = { "Snabble Store" })

    fun setupSutDto(widgetDto: WidgetDto?): DynamicConfigDto {
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

            val rootDto = setupSutDto(null)

            every { context.resolveImageId(rootDto.configuration.image) } returns 1

            val sut = createMapper().mapDtoToItems(rootDto)

            "configurationDto to ConfigurationItem" - {

                "image" {
                    sut.configuration.image shouldBe 1
                }
                "padding" {
                    sut.configuration.padding shouldBe Padding(0, 0, 0, 0)
                }
                "style" {
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
                    image = "R.drawable.abc",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                every { context.resolveImageId(imageDto.image) } returns 1

                val rootDto = setupSutDto(imageDto)
                val sut = createMapper().mapDtoToItems(rootDto)

                val imageItem = sut.widgets.first().shouldBeTypeOf<ImageItem>()

                "id" {
                    imageItem.id shouldBe "an.image"
                }
                "image" {
                    imageItem.image shouldBe 1
                }
                "padding" {
                    imageItem.padding shouldBe Padding(0, 0, 0, 0)
                }

            }

            "TextDto to TextItem" - {

                val textDto = TextDto(
                    id = "a.title",
                    text = "Hello World",
                    textColor = "asd",
                    textStyle = null,
                    showDisclosure = null,
                    PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(textDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 5
                every { context.getComposeColor(textDto.textColor) } returns 8
                val sut = createMapper().mapDtoToItems(rootDto)

                val textItem = sut.widgets.first().shouldBeTypeOf<TextItem>()


                "id" {
                    textItem.id shouldBe "a.title"
                }
                "text" {
                    textItem.text shouldBe "Hello World"
                }
                "text color" {
                    textItem.textColor shouldBe 8
                }
                "text style" {
                    textItem.textStyle shouldBe null
                }
                "padding" {
                    textItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "AppUserIdDto to AppUserIdItem" - {

                val appUserIdDto = AppUserIdDto(
                    id = "a.appUserId",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(appUserIdDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val appUserIdItem = sut.widgets.first().shouldBeTypeOf<AppUserIdItem>()

                "id" {
                    appUserIdItem.id shouldBe "a.appUserId"
                }

                "padding" {
                    appUserIdItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "ButtonDto to ButtonItem" - {

                val buttonDto = ButtonDto(
                    id = "a.button",
                    text = "Hello World",
                    foregroundColor = "test",
                    backgroundColor = null,
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(buttonDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1
                every { context.getResourceString(buttonDto.text) } returns "Hello World"
                every { context.resolveColorId(buttonDto.foregroundColor) } returns 2
                every { context.resolveColorId(buttonDto.backgroundColor) } returns 3

                val sut = createMapper().mapDtoToItems(rootDto)

                val buttonItem = sut.widgets.first().shouldBeTypeOf<ButtonItem>()

                "id" {
                    buttonItem.id shouldBe "a.button"
                }
                "text" {
                    buttonItem.text shouldBe "Hello World"
                }
                "foreground color" {
                    buttonItem.foregroundColor shouldBe 2
                }
                "background color" {
                    buttonItem.backgroundColor shouldBe 3
                }
                "padding" {
                    buttonItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "ClientIdDto to ClientIdItem" - {

                val clientIdDto = ClientIdDto(
                    id = "a.clientId",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(clientIdDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val clientIdItem = sut.widgets.first().shouldBeTypeOf<ClientIdItem>()

                "id" {
                    clientIdItem.id shouldBe "a.clientId"
                }

                "padding" {
                    clientIdItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "DevSettingsDto to DevSettingsItem" - {

                val devSettingsDto = DevSettingsDto(
                    id = "a.devSettings",
                    text = "devSettings",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(devSettingsDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val devSettingsItem = sut.widgets.first().shouldBeTypeOf<DevSettingsItem>()

                "id" {
                    devSettingsItem.id shouldBe "a.devSettings"
                }

                "text"{
                    devSettingsItem.text shouldBe "devSettings"
                }

                "padding" {
                    devSettingsItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }



            "LocationPermissionDto to LocationPermissionItem" - {

                val locationPermissionDto = LocationPermissionDto(
                    id = "a.location",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(locationPermissionDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val locationPermissionItem = sut.widgets.first().shouldBeTypeOf<LocationPermissionItem>()

                "id" {
                    locationPermissionItem.id shouldBe "a.location"
                }
                "padding" {
                    locationPermissionItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "SeeAllStoresDto to SeeAllStoresItem" - {

                val seeAllStoreDto = SeeAllStoresDto(
                    id = "a.store",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(seeAllStoreDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val seeAllStoresItem = sut.widgets.first().shouldBeTypeOf<SeeAllStoresItem>()

                "id" {
                    seeAllStoresItem.id shouldBe "a.store"
                }
                "padding" {
                    seeAllStoresItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "StartShoppingDto to StartShoppingItem" - {

                val startShoppingDto = StartShoppingDto(
                    id = "a.start",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(startShoppingDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val startShoppingItem = sut.widgets.first().shouldBeTypeOf<StartShoppingItem>()

                "id" {
                    startShoppingItem.id shouldBe "a.start"
                }
                "padding" {
                    startShoppingItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "InformationDto to InformationItem" - {

                val informationDto = InformationDto(
                    id = "a.info",
                    text = "information",
                    image = null,
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(informationDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1
                every { context.resolveImageId(informationDto.image) } returns 2

                val sut = createMapper().mapDtoToItems(rootDto)

                val informationItem = sut.widgets.first().shouldBeTypeOf<InformationItem>()

                "id" {
                    informationItem.id shouldBe "a.info"
                }
                "text" {
                    informationItem.text shouldBe "information"
                }
                "image" {
                    informationItem.image shouldBe 2
                }
                "padding" {
                    informationItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "CustomerCardDto to CustomerCardItem" - {

                val customerCardDto = CustomerCardDto(
                    id = "a.card",
                    text = "customerCard",
                    image = null,
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(customerCardDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1
                every { context.resolveImageId(customerCardDto.image) } returns 2

                val sut = createMapper().mapDtoToItems(rootDto)

                val customerCardItem = sut.widgets.first().shouldBeTypeOf<CustomerCardItem>()

                "id" {
                    customerCardItem.id shouldBe "a.card"
                }
                "text" {
                    customerCardItem.text shouldBe "customerCard"
                }
                "image" {
                    customerCardItem.image shouldBe 2
                }
                "padding" {
                    customerCardItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "ConnectWifiDto to ConnectWifiItem" - {

                val connectWlanDto = ConnectWlanDto(
                    id = "a.wifi",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(connectWlanDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val connectWlanItem = sut.widgets.first().shouldBeTypeOf<ConnectWlanItem>()

                "id" {
                    connectWlanItem.id shouldBe "a.wifi"
                }

                "padding" {
                    connectWlanItem.padding shouldBe Padding(0, 0, 0, 0)
                }

                "ssid" {
                    connectWlanItem.ssid shouldBe "Snabble Store"
                }
            }

            "SectionDto to SectionItem" - {

                val sectionDto = SectionDto(
                    id = "a.section",
                    header = "test",
                    widgets = listOf(
                        ConnectWlanDto(
                            id = "a.wifi",
                            padding = PaddingDto(0, 0, 0, 0)
                        )
                    ),
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(sectionDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val sectionItem = sut.widgets.first().shouldBeTypeOf<SectionItem>()

                "id" {
                    sectionItem.id shouldBe "a.section"
                }
                "header" {
                    sectionItem.header shouldBe "test"
                }
                "items" {
                    sectionItem.items.first().shouldBeTypeOf<ConnectWlanItem>()
                }
                "padding" {
                    sectionItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }

            "VersionDto to VersionItem" - {

                val versionDto = VersionDto(
                    id = "a.version",
                    padding = PaddingDto(0, 0, 0, 0)
                )

                val rootDto = setupSutDto(versionDto)
                every { context.resolveImageId(rootDto.configuration.image) } returns 1

                val sut = createMapper().mapDtoToItems(rootDto)

                val startShoppingItem = sut.widgets.first().shouldBeTypeOf<VersionItem>()

                "id" {
                    startShoppingItem.id shouldBe "a.version"
                }
                "padding" {
                    startShoppingItem.padding shouldBe Padding(0, 0, 0, 0)
                }
            }
        }
    }
})

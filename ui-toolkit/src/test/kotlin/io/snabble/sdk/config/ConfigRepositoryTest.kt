package io.snabble.sdk.config

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.snabble.sdk.data.ButtonDto
import io.snabble.sdk.data.ConfigurationDto
import io.snabble.sdk.data.CustomerCardDto
import io.snabble.sdk.data.ImageDto
import io.snabble.sdk.data.InformationDto
import io.snabble.sdk.data.LocationPermissionDto
import io.snabble.sdk.data.PaddingDto
import io.snabble.sdk.data.PurchasesDto
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.data.SectionDto
import io.snabble.sdk.data.SeeAllStoresDto
import io.snabble.sdk.data.StartShoppingDto
import io.snabble.sdk.data.TextDto
import io.snabble.sdk.data.ToggleDto
import kotlinx.serialization.json.Json

internal class ConfigRepositoryTest : FreeSpec({

    fun createJson(widgetsJson: String = "[]"): String = """{
              "configuration": {
                "image": "home_default_background",
                "style": "scroll"
              },
              "widgets": $widgetsJson
            }"""

    fun createSut(json: String) = ConfigRepository(
        fileProvider = mockk { coEvery { getFile(any()) } returns json },
        json = Json { ignoreUnknownKeys = true }
    )

    "Parsing" - {

        "the configuration object" {
            val sut = createSut(createJson())
            val config: RootDto = sut.getConfig("")

            config.configuration shouldBe ConfigurationDto(
                image = "home_default_background",
                style = "scroll"
            )
        }

        "a configuration w/ widgets being empty" {
            val sut = createSut(createJson())

            sut.getConfig<RootDto>("").widgets.shouldBeEmpty()
        }

        "a widget configuration w/" - {

            "a button" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "button",
                            "id": "submit",
                            "text": "Submit",
                            "foregroundColorSource": "white",
                            "backgroundColorSource": "blue",
                            "padding": [ 16, 4 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe ButtonDto(
                    id = "submit",
                    text = "Submit",
                    foregroundColorSource = "white",
                    backgroundColorSource = "blue",
                    padding = PaddingDto(start = 16, top = 4, end = 16, bottom = 4)
                )
            }

            "an image" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "image",
                            "id": "logo",
                            "imageSource": "snabble_logo",
                            "padding": [ 4 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe ImageDto(
                    id = "logo",
                    imageSource = "snabble_logo",
                    padding = PaddingDto(4, 4, 4, 4)
                )
            }

            "an information" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "information",
                            "id": "info",
                            "text": "Some useful information here.",
                            "imageSource": "snabble_logo",
                            "padding": [ 16, 12, 4, 16 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe InformationDto(
                    id = "info",
                    text = "Some useful information here.",
                    imageSource = "snabble_logo",
                    padding = PaddingDto(start = 16, top = 12, end = 16, bottom = 4)
                )
            }

            "a customer card"{
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "snabble.customerCard",
                            "id": "info",
                            "text": "Some useful information here.",
                            "imageSource": "snabble_logo",
                            "padding": [ 16, 12, 4, 16 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe CustomerCardDto(
                    id = "info",
                    text = "Some useful information here.",
                    imageSource = "snabble_logo",
                    padding = PaddingDto(start = 16, top = 12, end = 16, bottom = 4)
                )
            }

            "a location permission widget" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "snabble.locationPermission",
                            "id": "location_permission",
                            "padding": [ 16 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe LocationPermissionDto(
                    id = "location_permission",
                    padding = PaddingDto(16, 16, 16, 16)
                )
            }

            "a purchases widget" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "purchases",
                            "id": "purchases",
                            "projectId": "ab1234",
                            "padding": [ 16 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe PurchasesDto(
                    id = "purchases",
                    projectId = "ab1234",
                    padding = PaddingDto(16, 16, 16, 16)
                )
            }

            "a section" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "section",
                            "id": "section1",
                            "header": "Settings",
                            "items": [],
                            "padding": [ 8 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe SectionDto(
                    id = "section1",
                    header = "Settings",
                    widgets = emptyList(),
                    padding = PaddingDto(8, 8, 8, 8)
                )
            }

            "a see all stores widget" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "snabble.allStores",
                            "id": "all_stores",
                            "padding": [ 8 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe SeeAllStoresDto(
                    id = "all_stores",
                    padding = PaddingDto(8, 8, 8, 8)
                )
            }

            "a start shopping widget" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "snabble.startShopping",
                            "id": "start_shopping",
                            "padding": [ 8 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe StartShoppingDto(
                    id = "start_shopping",
                    padding = PaddingDto(8, 8, 8, 8)
                )
            }

            "a text" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "text",
                            "id": "title",
                            "text": "Hello World!",
                            "textColor": "black",
                            "textStyle": "body",
                            "showDisclosure": false,
                            "padding": [ 8 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe TextDto(
                    id = "title",
                    text = "Hello World!",
                    textColorSource = "black",
                    textStyleSource = "body",
                    showDisclosure = false,
                    padding = PaddingDto(8, 8, 8, 8)
                )
            }

            "a toggle" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "toggle",
                            "id": "onboarding_toggle",
                            "text": "Show Onboarding",
                            "key": "show_onboarding",
                            "padding": [ 8 ]
                          }]"""
                    )
                )
                sut.getConfig<RootDto>("").widgets.first() shouldBe ToggleDto(
                    id = "onboarding_toggle",
                    text = "Show Onboarding",
                    key = "show_onboarding",
                    padding = PaddingDto(8, 8, 8, 8)
                )
            }
        }
    }
})

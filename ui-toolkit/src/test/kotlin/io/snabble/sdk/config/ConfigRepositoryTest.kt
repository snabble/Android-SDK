package io.snabble.sdk.config

import android.content.Context
import android.graphics.Color
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.ConfigMapperImpl
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.ConnectWifiItem
import io.snabble.sdk.domain.CustomerCardItem
import io.snabble.sdk.domain.DynamicConfig
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.ProjectId
import io.snabble.sdk.domain.PurchasesItem
import io.snabble.sdk.domain.SectionItem
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.domain.ToggleItem
import kotlinx.serialization.json.Json

internal class ConfigRepositoryTest : FreeSpec({

    fun createJson(widgetsJson: String = "[]"): String = """{
              "configuration": {
                "image": "home_default_background",
                "style": "scroll",
                "padding": [ 16 ]
              },
              "widgets": $widgetsJson
            }"""

    fun createSut(json: String, mapping: Context.() -> Unit = {}) = ConfigRepository(
        fileProvider = mockk { coEvery { getFile(any()) } returns json },
        json = Json { ignoreUnknownKeys = true },
        configMapper = ConfigMapperImpl(context = mockk(relaxed = true, block = mapping)),
    )

    beforeEach {
        clearAllMocks()
    }

    "Parsing" - {

        "the configuration object" {
            val sut = createSut(createJson()) {
                every { resources.getIdentifier("home_default_background", any(), any()) } returns 42
            }
            val config: DynamicConfig = sut.getConfig("")

            config.configuration shouldBe Configuration(
                image = 42,
                style = "scroll",
                padding = Padding(16, 16, 16, 16)
            )
        }

        "a configuration w/ widgets being empty" {
            val sut = createSut(createJson())

            sut.getConfig("").widgets.shouldBeEmpty()
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
                ) {
                    every { resources.getIdentifier("Submit", any(), any()) } returns 1
                    every { resources.getText(1) } returns "Submit"
                    every { resources.getIdentifier("white", any(), any()) } returns 7
                    every { resources.getIdentifier("blue", any(), any()) } returns 21
                }
                sut.getConfig("").widgets.first() shouldBe ButtonItem(
                    id = "submit",
                    text = "Submit",
                    foregroundColorSource = 7,
                    backgroundColorSource = 21,
                    padding = Padding(start = 16, top = 4, end = 16, bottom = 4)
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
                ) {
                    every { resources.getIdentifier("snabble_logo", any(), any()) } returns 7
                }
                sut.getConfig("").widgets.first() shouldBe ImageItem(
                    id = "logo",
                    imageSource = 7,
                    padding = Padding(4, 4, 4, 4)
                )
            }

            "an information" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "information",
                            "id": "info",
                            "text": "info_description",
                            "imageSource": "snabble_logo",
                            "padding": [ 16, 12, 4, 16 ]
                          }]"""
                    )
                ) {
                    every { resources.getIdentifier("info_description", any(), any()) } returns 2
                    every { resources.getText(2) } returns "Some useful information here."
                    every { resources.getIdentifier("snabble_logo", any(), any()) } returns 7
                }
                sut.getConfig("").widgets.first() shouldBe InformationItem(
                    id = "info",
                    text = "Some useful information here.",
                    imageSource = 7,
                    padding = Padding(start = 16, top = 12, end = 16, bottom = 4)
                )
            }

            "a customer card"{
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "snabble.customerCard",
                            "id": "info",
                            "text": "card_info",
                            "imageSource": "snabble_logo",
                            "padding": [ 16, 12, 4, 16 ]
                          }]"""
                    )
                ) {
                    every { resources.getIdentifier("card_info", any(), any()) } returns 3
                    every { resources.getText(3) } returns "Some useful information here."
                    every { resources.getIdentifier("snabble_logo", any(), any()) } returns 7
                }
                sut.getConfig("").widgets.first() shouldBe CustomerCardItem(
                    id = "info",
                    text = "Some useful information here.",
                    imageSource = 7,
                    padding = Padding(start = 16, top = 12, end = 16, bottom = 4)
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
                sut.getConfig("").widgets.first() shouldBe LocationPermissionItem(
                    id = "location_permission",
                    padding = Padding(16, 16, 16, 16)
                )
            }

            "a connect wifi widget" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "snabble.connectWifi",
                            "id": "connect_wifi",
                            "padding": [ 16 ]
                          }]"""
                    )
                )
                sut.getConfig("").widgets.first() shouldBe ConnectWifiItem(
                    id = "connect_wifi",
                    padding = Padding(16, 16, 16, 16)
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
                sut.getConfig("").widgets.first() shouldBe PurchasesItem(
                    id = "purchases",
                    projectId = ProjectId("ab1234"),
                    padding = Padding(16, 16, 16, 16)
                )
            }

            "a section" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "section",
                            "id": "section1",
                            "header": "section1_header",
                            "items": [],
                            "padding": [ 8 ]
                          }]"""
                    )
                ) {
                    every { resources.getIdentifier("section1_header", any(), any()) } returns 4
                    every { resources.getText(4) } returns "Settings"
                }
                sut.getConfig("").widgets.first() shouldBe SectionItem(
                    id = "section1",
                    header = "Settings",
                    items = emptyList(),
                    padding = Padding(8, 8, 8, 8)
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
                sut.getConfig("").widgets.first() shouldBe SeeAllStoresItem(
                    id = "all_stores",
                    padding = Padding(8, 8, 8, 8)
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
                sut.getConfig("").widgets.first() shouldBe StartShoppingItem(
                    id = "start_shopping",
                    padding = Padding(8, 8, 8, 8)
                )
            }

            "a text" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "text",
                            "id": "title",
                            "text": "hello_world_text",
                            "textColor": "black",
                            "textStyle": "body",
                            "showDisclosure": false,
                            "padding": [ 8 ]
                          }]"""
                    )
                ) {
                    every { resources.getIdentifier("hello_world_text", any(), any()) } returns 5
                    every { resources.getText(5) } returns "Hello World!"
                    every { resources.getIdentifier("black", any(), any()) } returns 999
                }
                mockkStatic(Color::class)
                every { Color.parseColor(any()) } returns 5
                sut.getConfig("").widgets.first() shouldBe TextItem(
                    id = "title",
                    text = "Hello World!",
                    textColorSource = 5,
                    textStyleSource = "body",
                    showDisclosure = false,
                    padding = Padding(8, 8, 8, 8)
                )
            }

            "a toggle" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "toggle",
                            "id": "onboarding_toggle",
                            "text": "onboarding_title",
                            "key": "show_onboarding",
                            "padding": [ 8 ]
                          }]"""
                    )
                ) {
                    every { resources.getIdentifier("onboarding_title", any(), any()) } returns 6
                    every { resources.getText(6) } returns "Show Onboarding"
                }
                sut.getConfig("").widgets.first() shouldBe ToggleItem(
                    id = "onboarding_toggle",
                    text = "Show Onboarding",
                    key = "show_onboarding",
                    padding = Padding(8, 8, 8, 8)
                )
            }
        }
    }
})

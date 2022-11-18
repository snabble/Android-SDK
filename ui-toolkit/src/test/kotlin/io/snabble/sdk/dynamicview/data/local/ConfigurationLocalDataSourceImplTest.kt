package io.snabble.sdk.dynamicview.data.local

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
import io.snabble.sdk.dynamicview.data.dto.mapper.ConfigMapperImpl
import io.snabble.sdk.dynamicview.domain.model.ButtonItem
import io.snabble.sdk.dynamicview.domain.model.Configuration
import io.snabble.sdk.dynamicview.domain.model.ConnectWlanItem
import io.snabble.sdk.dynamicview.domain.model.CustomerCardItem
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import io.snabble.sdk.dynamicview.domain.model.ImageItem
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.PurchasesItem
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.SeeAllStoresItem
import io.snabble.sdk.dynamicview.domain.model.StartShoppingItem
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import kotlinx.serialization.json.Json

internal class ConfigurationLocalDataSourceImplTest : FreeSpec({

    fun createJson(widgetsJson: String = "[]"): String = """{
              "configuration": {
                "image": "home_default_background",
                "style": "scroll",
                "padding": [ 16 ]
              },
              "widgets": $widgetsJson
            }"""

    fun createSut(
        json: String,
        mapping: Context.() -> Unit = {},
    ) = ConfigurationLocalDataSourceImpl(
        fileProvider = mockk { coEvery { getFile(any()) } returns json },
        json = Json { ignoreUnknownKeys = true },
        configMapper = ConfigMapperImpl(
            context = mockk(relaxed = true, block = mapping),
            ssidProvider = { "Snabble Store" }
        ),
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
                            "foregroundColor": "white",
                            "backgroundColor": "blue",
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
                    foregroundColor = 7,
                    backgroundColor = 21,
                    padding = Padding(start = 16, top = 4, end = 16, bottom = 4)
                )
            }

            "an image" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "image",
                            "id": "logo",
                            "image": "snabble_logo",
                            "padding": [ 4 ]
                          }]"""
                    )
                ) {
                    every { resources.getIdentifier("snabble_logo", any(), any()) } returns 7
                }
                sut.getConfig("").widgets.first() shouldBe ImageItem(
                    id = "logo",
                    image = 7,
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
                            "image": "snabble_logo",
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
                    image = 7,
                    padding = Padding(start = 16, top = 12, end = 16, bottom = 4)
                )
            }

            "a customer card" {
                val sut = createSut(
                    createJson(
                        """[{
                            "type": "snabble.customerCard",
                            "id": "info",
                            "text": "card_info",
                            "image": "snabble_logo",
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
                    image = 7,
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
                sut.getConfig("").widgets.first() shouldBe ConnectWlanItem(
                    id = "connect_wifi",
                    padding = Padding(16, 16, 16, 16),
                    ssid = "Snabble Store"
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
                    textColor = 5,
                    textStyle = "body",
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

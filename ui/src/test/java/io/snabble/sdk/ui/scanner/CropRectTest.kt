package io.snabble.sdk.ui.scanner

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CropRectTest : FreeSpec({

    "An image w/ the size 1280x720" - {

        "should produce a CropRect w/ scanRectHeight" - {

            "1/2 of the size 640x720" {
                val cropRect = CropRect.from(width = 1280, height = 720, scanRectHeight = 1 / 2f)

                cropRect shouldBe CropRect(left = 320, top = 0, right = 960, bottom = 720)
            }

            "2/3 of the size 853x720" {
                val cropRect = CropRect.from(width = 1280, height = 720, scanRectHeight = 2 / 3f)

                cropRect shouldBe CropRect(left = 213, top = 0, right = 1066, bottom = 720)
            }
        }

    }

    "An image w/ the size 720x1280" - {

        "should produce a crop rect w/ scanRectHeight" - {

            "1/2 of the size 720x640" {
                val cropRect = CropRect.from(width = 720, height = 1280, scanRectHeight = 1 / 2f)

                cropRect shouldBe CropRect(left = 0, top = 320, right = 720, bottom = 960)
            }

            "1/3 of the size 720x853" {
                val cropRect = CropRect.from(width = 720, height = 1280, scanRectHeight = 2 / 3f)

                cropRect shouldBe CropRect(left = 0, top = 213, right = 720, bottom = 1066)
            }
        }
    }
})

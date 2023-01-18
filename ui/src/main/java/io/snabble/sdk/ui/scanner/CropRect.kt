package io.snabble.sdk.ui.scanner

import android.graphics.Rect

data class CropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {

    companion object {

        fun from(
            width: Int,
            height: Int,
            scanRectHeight: Float
        ): CropRect {
            val actualWidth: Int = maxOf(width, height)
            val actualHeight: Int = minOf(width, height)

            val scanAreaStart = actualWidth * CENTER_FRACTION - actualWidth * scanRectHeight * CENTER_FRACTION
            val scanAreaEnd = actualWidth * CENTER_FRACTION + actualWidth * scanRectHeight * CENTER_FRACTION

            val isLandscape = width >= height
            return if (isLandscape) {
                CropRect(scanAreaStart.toInt(), 0, scanAreaEnd.toInt(), actualHeight)
            } else {
                CropRect(0, scanAreaStart.toInt(), actualHeight, scanAreaEnd.toInt())
            }
        }

        private const val CENTER_FRACTION = .5f
    }
}

fun CropRect.toRect(): Rect = Rect(this.left, this.top, this.right, this.bottom)

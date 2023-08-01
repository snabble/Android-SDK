package io.snabble.sdk.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrCodeGenerator {

    fun generateQrCodeFrom(token: String): Bitmap {
        val hintMap: Map<EncodeHintType, Any> = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            EncodeHintType.MARGIN to 0
        )
        val qrCodeWriter = QRCodeWriter()
        val bitMapMatrix = qrCodeWriter.encode(
            token,
            BarcodeFormat.QR_CODE,
            QR_CODE_WIDTH,
            QR_CODE_HEIGHT,
            hintMap
        )

        val width = bitMapMatrix.width
        val height = bitMapMatrix.height

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] =
                    if (bitMapMatrix.get(x, y)) Color.BLACK else Color.TRANSPARENT
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    companion object {

        private const val QR_CODE_WIDTH = 400
        private const val QR_CODE_HEIGHT = 400

        fun generateQrCode(token: String) =
            QrCodeGenerator().generateQrCodeFrom(token)
    }
}

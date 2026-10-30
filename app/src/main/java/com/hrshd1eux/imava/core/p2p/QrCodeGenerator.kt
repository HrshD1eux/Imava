package com.hrshd1eux.imava.core.p2p

import android.graphics.Bitmap
import android.graphics.Color

object QrCodeGenerator {

    /**
     * Generates a monochrome QR Code Bitmap from raw text.
     * Pure Kotlin, zero Google libraries, zero external dependencies.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 420,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            PureQrCodeEncoder.encodeToBitmap(
                content = content,
                targetSizePx = sizePx,
                foregroundColor = foregroundColor,
                backgroundColor = backgroundColor
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

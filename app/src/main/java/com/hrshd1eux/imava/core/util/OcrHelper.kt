package com.hrshd1eux.imava.core.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

data class OcrBlock(
    val text: String,
    val boundingBox: android.graphics.Rect? = null
)

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>
)

object OcrHelper {
    suspend fun recognizeTextFromUri(context: Context, uri: Uri): OcrResult? = null
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): OcrResult? = null
}

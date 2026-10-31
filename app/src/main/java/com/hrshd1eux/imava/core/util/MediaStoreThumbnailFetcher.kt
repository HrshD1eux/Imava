package com.hrshd1eux.imava.core.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance hardware thumbnail fetcher that delegates to Android OS
 * ContentResolver.loadThumbnail() on Android 10+ (API 29+).
 * 
 * Bypasses full-resolution image decoding (e.g. 50MP files) during grid scrolling,
 * loading Android's pre-rendered hardware thumbnails in 1-2ms.
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null

        val width = options.size.width
        val height = options.size.height

        // If original size or undefined dimensions were requested, let the full decoder handle it
        if (width !is coil.size.Dimension.Pixels || height !is coil.size.Dimension.Pixels) {
            return@withContext null
        }

        val widthPx = width.px
        val heightPx = height.px

        // Only handle thumbnail sizes (e.g. grid items); let full photo viewer load original high-res stream
        if (widthPx > 1080 || heightPx > 1080) return@withContext null

        try {
            val targetSize = Size(widthPx.coerceIn(128, 1080), heightPx.coerceIn(128, 1080))
            val bitmap = context.contentResolver.loadThumbnail(data, targetSize, null)
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } catch (_: Exception) {
            null // Fallback to standard Coil pipeline if thumbnail is unavailable or corrupt
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            val isMediaStoreUri = data.scheme == ContentResolver.SCHEME_CONTENT &&
                (data.authority == MediaStore.AUTHORITY || data.authority?.contains("media") == true)
            if (!isMediaStoreUri) return null

            return MediaStoreThumbnailFetcher(context, data, options)
        }
    }
}

package com.hrshd1eux.imava.core.util

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperUtil {

    const val WALLPAPER_HOME = WallpaperManager.FLAG_SYSTEM
    const val WALLPAPER_LOCK = WallpaperManager.FLAG_LOCK
    const val WALLPAPER_BOTH = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK

    suspend fun setWallpaperDirect(context: Context, uri: Uri, which: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        wallpaperManager.setStream(stream, null, true, which)
                        return@withContext true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, which)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }
                    bitmap.recycle()
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun setWallpaperCropped(
        context: Context,
        uri: Uri,
        zoomScale: Float,
        panOffsetX: Float,
        panOffsetY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        which: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val fullBitmap = BitmapFactory.decodeStream(stream, null, options) ?: return@withContext false

                val orientedBitmap = fixOrientation(context, uri, fullBitmap)

                val bmpW = orientedBitmap.width.toFloat()
                val bmpH = orientedBitmap.height.toFloat()

                val targetAspect = (viewportWidth / viewportHeight.coerceAtLeast(1f)).coerceIn(0.2f, 5f)
                val baseCropW: Float
                val baseCropH: Float
                if (bmpW / bmpH > targetAspect) {
                    baseCropH = bmpH
                    baseCropW = bmpH * targetAspect
                } else {
                    baseCropW = bmpW
                    baseCropH = bmpW / targetAspect
                }

                val effectiveCropW = (baseCropW / zoomScale.coerceAtLeast(1f)).coerceIn(10f, bmpW)
                val effectiveCropH = (baseCropH / zoomScale.coerceAtLeast(1f)).coerceIn(10f, bmpH)

                val maxPanX = (bmpW - effectiveCropW) / 2f
                val maxPanY = (bmpH - effectiveCropH) / 2f
                val centerX = bmpW / 2f - (panOffsetX * maxPanX)
                val centerY = bmpH / 2f - (panOffsetY * maxPanY)

                val cropLeft = (centerX - effectiveCropW / 2f).toInt().coerceIn(0, (bmpW - effectiveCropW).toInt().coerceAtLeast(0))
                val cropTop = (centerY - effectiveCropH / 2f).toInt().coerceIn(0, (bmpH - effectiveCropH).toInt().coerceAtLeast(0))
                val cropW = effectiveCropW.toInt().coerceIn(1, (orientedBitmap.width - cropLeft).coerceAtLeast(1))
                val cropH = effectiveCropH.toInt().coerceIn(1, (orientedBitmap.height - cropTop).coerceAtLeast(1))

                val cropped = Bitmap.createBitmap(orientedBitmap, cropLeft, cropTop, cropW, cropH)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(cropped, null, true, which)
                } else {
                    wallpaperManager.setBitmap(cropped)
                }

                if (cropped != orientedBitmap && !cropped.isRecycled) cropped.recycle()
                if (orientedBitmap != fullBitmap && !orientedBitmap.isRecycled) orientedBitmap.recycle()
                if (!fullBitmap.isRecycled) fullBitmap.recycle()
                return@withContext true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun fixOrientation(context: Context, uri: Uri, src: Bitmap): Bitmap {
        return try {
            var rotationDegrees = 0
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
            } else {
                src
            }
        } catch (_: Exception) {
            src
        }
    }

    fun launchPixelOrSystemWallpaperPicker(activity: Activity, uri: Uri): Boolean {
        val pixelPackages = listOf(
            "com.google.android.apps.wallpaper",
            "com.google.android.apps.wallpaper.nexus",
            "com.android.wallpaper"
        )
        for (pkg in pixelPackages) {
            try {
                val intent = Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER).apply {
                    setDataAndType(uri, "image/*")
                    putExtra("mimeType", "image/*")
                    setPackage(pkg)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (intent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }

        try {
            val wallpaperManager = WallpaperManager.getInstance(activity)
            val intent = wallpaperManager.getCropAndSetWallpaperIntent(uri)
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
                return true
            }
        } catch (_: Exception) {}

        // System Wallpaper & Style Settings (never opens generic contacts/whatsapp chooser)
        val styleIntents = listOf(
            Intent("android.intent.action.SET_WALLPAPER"),
            Intent("android.settings.WALLPAPER_SETTINGS"),
            Intent(Intent.ACTION_SET_WALLPAPER)
        )
        for (intent in styleIntents) {
            try {
                if (intent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }

        return false
    }

    fun setAsContactPhoto(activity: Activity, uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                setDataAndType(uri, "image/*")
                putExtra("mimeType", "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "Set as Contact Photo"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

package com.hrshd1eux.imava

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ImavaApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        try {
            androidx.work.WorkManager.getInstance(this).cancelUniqueWork("TrashAutoPurgeWork")
        } catch (_: Exception) {}
        scheduleAppUpdateChecker()
    }

    private fun scheduleAppUpdateChecker() {
        try {
            val updateCheckRequest = androidx.work.PeriodicWorkRequestBuilder<com.hrshd1eux.imava.core.worker.AppUpdateCheckWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            ).build()

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ImavaAppUpdateCheckWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                updateCheckRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(com.hrshd1eux.imava.core.util.VaultFetcher.Factory())
                add(VideoFrameDecoder.Factory())
                add(GifDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}

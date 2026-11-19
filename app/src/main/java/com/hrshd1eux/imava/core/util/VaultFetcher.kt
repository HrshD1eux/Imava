package com.hrshd1eux.imava.core.util

import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.CachePolicy
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.io.File

class VaultFetcher(
    private val file: File,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): SourceResult? {
        val buffer = Buffer()
        val success = withContext(Dispatchers.IO) {
            try {
                file.inputStream().use { input ->
                    VaultCrypto.decrypt(input, buffer.outputStream())
                }
                true
            } catch (e: Exception) {
                // Fallback for unencrypted legacy vault file if present
                try {
                    file.inputStream().use { input ->
                        input.copyTo(buffer.outputStream())
                    }
                    true
                } catch (ex: Exception) {
                    false
                }
            }
        }

        if (!success || buffer.size == 0L) return null

        val mimeType = when {
            buffer.rangeEquals(0, okio.ByteString.of(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) -> "image/jpeg"
            buffer.rangeEquals(0, okio.ByteString.of(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())) -> "image/png"
            buffer.rangeEquals(0, okio.ByteString.of('G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte())) -> "image/gif"
            buffer.rangeEquals(0, okio.ByteString.of('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte())) -> "image/webp"
            else -> null
        }

        val imageSource = ImageSource(source = buffer, context = options.context)

        return SourceResult(
            source = imageSource,
            mimeType = mimeType,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val path = data.path ?: return null
            if (path.contains("vault_cache")) return null
            if ((path.contains("/vault/") || path.contains("/decoy_vault/") || path.contains("vault_") || data.scheme == "vault") && !path.endsWith(".meta")) {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    // no caching for vault media
                    val vaultOptions = options.copy(
                        diskCachePolicy = CachePolicy.DISABLED,
                        memoryCachePolicy = CachePolicy.DISABLED
                    )
                    return VaultFetcher(file, vaultOptions)
                }
            }
            return null
        }
    }
}

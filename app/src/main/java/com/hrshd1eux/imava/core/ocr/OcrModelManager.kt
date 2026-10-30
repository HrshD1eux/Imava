package com.hrshd1eux.imava.core.ocr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object OcrModelManager {

    private const val MODEL_FILENAME = "eng.traineddata"
    private const val TESSDATA_DIR = "tessdata"
    private const val MODEL_URL = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/eng.traineddata"
    private const val MIN_EXPECTED_SIZE = 3_000_000L // ~4.1 MB expected

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun getTessDataDir(context: Context): File {
        val dir = File(context.filesDir, TESSDATA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getModelFile(context: Context): File {
        return File(getTessDataDir(context), MODEL_FILENAME)
    }

    fun isModelReady(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() >= MIN_EXPECTED_SIZE
    }

    fun getModelSizeBytes(context: Context): Long {
        val file = getModelFile(context)
        return if (file.exists()) file.length() else 0L
    }

    fun deleteModel(context: Context): Boolean {
        val file = getModelFile(context)
        return if (file.exists()) file.delete() else true
    }

    suspend fun downloadModel(
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getModelFile(context)
        val tempFile = File(getTessDataDir(context), "$MODEL_FILENAME.tmp")

        try {
            val request = Request.Builder()
                .url(MODEL_URL)
                .header("User-Agent", "Imava-Android-Gallery")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            onProgress(progress.coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.length() < MIN_EXPECTED_SIZE) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Downloaded model is corrupt or incomplete (${tempFile.length()} bytes)"))
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }

            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(1f)
            Result.success(targetFile)
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Result.failure(e)
        }
    }
}

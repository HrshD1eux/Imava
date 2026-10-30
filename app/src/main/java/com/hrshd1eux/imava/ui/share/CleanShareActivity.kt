package com.hrshd1eux.imava.ui.share

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import com.hrshd1eux.imava.core.util.ExifSanitizerUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CleanShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val type = intent?.type

        if ((action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) && type != null) {
            handleIncomingShare(intent)
        } else {
            finish()
        }
    }

    private fun handleIncomingShare(incomingIntent: Intent) {
        val incomingUris = mutableListOf<Uri>()

        if (incomingIntent.action == Intent.ACTION_SEND) {
            val uri = incomingIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) incomingUris.add(uri)
        } else if (incomingIntent.action == Intent.ACTION_SEND_MULTIPLE) {
            val list = incomingIntent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (list != null) incomingUris.addAll(list)
        }

        if (incomingUris.isEmpty()) {
            Toast.makeText(this, "No media to clean", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(this, "Scrubbing GPS & metadata...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val cleanUris = withContext(Dispatchers.IO) {
                processAndScrubUris(incomingUris)
            }

            if (cleanUris.isEmpty()) {
                Toast.makeText(this@CleanShareActivity, "Failed to clean media", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val shareIntent = if (cleanUris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, cleanUris.first())
                    type = incomingIntent.type ?: "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(cleanUris))
                    type = incomingIntent.type ?: "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share Cleaned Media (No GPS/Metadata)")
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(chooser)
            finish()
        }
    }

    private fun processAndScrubUris(uris: List<Uri>): List<Uri> {
        val cleanDir = File(cacheDir, "clean_share_${System.currentTimeMillis()}").apply { mkdirs() }
        val outputUris = mutableListOf<Uri>()

        for ((index, uri) in uris.withIndex()) {
            try {
                val tempFile = File(cleanDir, "clean_${index}_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: continue

                // Strip all location, serial numbers, camera model, and user tags
                ExifSanitizerUtil.stripMetadata(tempFile)

                val contentUri = FileProvider.getUriForFile(
                    this,
                    "com.hrshd1eux.imava.fileprovider",
                    tempFile
                )
                outputUris.add(contentUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return outputUris
    }
}

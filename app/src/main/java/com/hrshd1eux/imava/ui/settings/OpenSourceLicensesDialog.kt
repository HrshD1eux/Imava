package com.hrshd1eux.imava.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val description: String,
    val url: String
)

@Composable
fun OpenSourceLicensesDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val libraries = remember {
        listOf(
            OpenSourceLibrary(
                name = "Jetpack Compose",
                author = "Google LLC",
                license = "Apache License 2.0",
                description = "Modern declarative UI toolkit for native Android development.",
                url = "https://developer.android.com/jetpack/compose"
            ),
            OpenSourceLibrary(
                name = "Kotlin & Coroutines",
                author = "JetBrains s.r.o. & Contributors",
                license = "Apache License 2.0",
                description = "Modern programming language and asynchronous coroutine primitives.",
                url = "https://github.com/Kotlin/kotlinx.coroutines"
            ),
            OpenSourceLibrary(
                name = "Coil Image Loader",
                author = "Colin White & Contributors",
                license = "Apache License 2.0",
                description = "Fast, lightweight, Kotlin-first image loading library for Android.",
                url = "https://github.com/coil-kt/coil"
            ),
            OpenSourceLibrary(
                name = "Hilt & Dagger",
                author = "Google LLC",
                license = "Apache License 2.0",
                description = "Compile-time dependency injection framework for Android.",
                url = "https://github.com/google/dagger"
            ),
            OpenSourceLibrary(
                name = "AndroidX Room Database",
                author = "Google LLC",
                license = "Apache License 2.0",
                description = "SQLite abstraction layer providing robust offline local-first storage.",
                url = "https://developer.android.com/training/data-storage/room"
            ),
            OpenSourceLibrary(
                name = "AndroidX Media3 ExoPlayer",
                author = "Google LLC",
                license = "Apache License 2.0",
                description = "Application-level media player for high-performance video streaming and playback.",
                url = "https://github.com/androidx/media"
            ),
            OpenSourceLibrary(
                name = "AndroidX Paging 3",
                author = "Google LLC",
                license = "Apache License 2.0",
                description = "Smooth memory-efficient pagination of large photo and video datasets.",
                url = "https://developer.android.com/topic/libraries/architecture/paging/v3-overview"
            ),
            OpenSourceLibrary(
                name = "AndroidX Security Crypto",
                author = "Google LLC",
                license = "Apache License 2.0",
                description = "Hardware-backed EncryptedSharedPreferences and master keys.",
                url = "https://developer.android.com/jetpack/androidx/releases/security"
            ),
            OpenSourceLibrary(
                name = "AndroidX Exifinterface",
                author = "Google LLC",
                license = "Apache License 2.0",
                description = "Fast metadata reader and writer for EXIF tags, GPS geotags, and camera details.",
                url = "https://developer.android.com/jetpack/androidx/releases/exifinterface"
            ),
            OpenSourceLibrary(
                name = "Material 3 Components",
                author = "Google LLC",
                license = "Apache License 2.0",
                description = "Next-generation Material You design system components.",
                url = "https://m3.material.io"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Open Source Licenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Used under free & open-source licenses",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(libraries) { lib ->
                    OutlinedCard(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lib.url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lib.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = lib.license,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "By ${lib.author}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lib.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "View project",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

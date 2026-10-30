package com.hrshd1eux.imava.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OpenSourceLibraryFull(
    val id: String,
    val name: String,
    val author: String,
    val licenseType: String,
    val description: String,
    val url: String,
    val fullLicenseText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    val apache2License = """
        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
    """.trimIndent()

    val gpl3License = """
        Imava is Free Software: you can redistribute it and/or modify it
        under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
        GNU General Public License for more details.
    """.trimIndent()

    val libraries = remember {
        listOf(
            OpenSourceLibraryFull(
                id = "imava",
                name = "Imava Android Gallery",
                author = "Harsh Pahal (HrshD1eux)",
                licenseType = "GNU General Public License v3.0",
                description = "Modern, high-performance, fully offline private Android gallery app.",
                url = "https://github.com/HrshD1eux/Imava",
                fullLicenseText = gpl3License
            ),
            OpenSourceLibraryFull(
                id = "compose",
                name = "Jetpack Compose",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Modern declarative UI toolkit for native Android development.",
                url = "https://developer.android.com/jetpack/compose",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "coroutines",
                name = "Kotlin & Coroutines",
                author = "JetBrains s.r.o. & Contributors",
                licenseType = "Apache License 2.0",
                description = "Modern language primitives for asynchronous, non-blocking execution.",
                url = "https://github.com/Kotlin/kotlinx.coroutines",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "coil",
                name = "Coil Image Loader",
                author = "Colin White & Contributors",
                licenseType = "Apache License 2.0",
                description = "Fast, lightweight, Kotlin-first image and GIF loading pipeline.",
                url = "https://github.com/coil-kt/coil",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "hilt",
                name = "Hilt & Dagger",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Compile-time dependency injection framework for Android.",
                url = "https://dagger.dev/hilt/",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "room",
                name = "Room Persistence Library",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Robust SQLite object mapping abstraction with compile-time query verification.",
                url = "https://developer.android.com/training/data-storage/room",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "paging",
                name = "AndroidX Paging 3",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Memory-efficient incremental media pagination with Flow integration.",
                url = "https://developer.android.com/topic/libraries/architecture/paging/v3-overview",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "media3",
                name = "AndroidX Media3 ExoPlayer",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Hardware-accelerated extensible media player engine for videos and audio.",
                url = "https://developer.android.com/media/media3",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "workmanager",
                name = "AndroidX WorkManager",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Guaranteed background task scheduler with power and battery constraints.",
                url = "https://developer.android.com/topic/libraries/architecture/workmanager",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "biometric",
                name = "AndroidX Biometric",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Biometric prompt dialog and hardware-backed credential authentication.",
                url = "https://developer.android.com/jetpack/androidx/releases/biometric",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "security",
                name = "AndroidX Security Crypto",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "EncryptedSharedPreferences and MasterKey hardware key wrapper.",
                url = "https://developer.android.com/jetpack/androidx/releases/security",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "exif",
                name = "AndroidX ExifInterface",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Metadata reader and writer for EXIF tags, GPS geotags, and camera details.",
                url = "https://developer.android.com/jetpack/androidx/releases/exifinterface",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "material3",
                name = "Material 3 Components",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Next-generation Material You design system components.",
                url = "https://m3.material.io",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "corektx",
                name = "AndroidX Core KTX",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Kotlin extensions for Android system APIs and backward-compatible core utilities.",
                url = "https://developer.android.com/jetpack/androidx/releases/core",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "activitycompose",
                name = "AndroidX Activity Compose",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "ComponentActivity extensions for Jetpack Compose integration and system back handling.",
                url = "https://developer.android.com/jetpack/androidx/releases/activity",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "lifecycle",
                name = "AndroidX Lifecycle Runtime KTX",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Lifecycle-aware components, coroutine scopes, and viewmodel state management.",
                url = "https://developer.android.com/jetpack/androidx/releases/lifecycle",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "palette",
                name = "AndroidX Palette KTX",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "Extracts vibrant and muted color profiles from images for adaptive UI rendering.",
                url = "https://developer.android.com/jetpack/androidx/releases/palette",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "print_fragment",
                name = "AndroidX Print & Fragment KTX",
                author = "Google LLC",
                licenseType = "Apache License 2.0",
                description = "System document printing pipeline and Kotlin fragment extensions.",
                url = "https://developer.android.com/jetpack/androidx/releases/print",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "okhttp",
                name = "OkHttp & Okio",
                author = "Square, Inc.",
                licenseType = "Apache License 2.0",
                description = "High-performance HTTP & WebDAV streaming client for local NAS backup and updates.",
                url = "https://square.github.io/okhttp/",
                fullLicenseText = apache2License
            ),
            OpenSourceLibraryFull(
                id = "tesseract",
                name = "Tesseract 5 & Leptonica (Tesseract4Android)",
                author = "Adaptech / Tesseract OCR Community",
                licenseType = "Apache License 2.0",
                description = "Open-source C++ optical character recognition engine and image processing library.",
                url = "https://github.com/adaptech-cz/Tesseract4Android",
                fullLicenseText = apache2License
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Open Source Licenses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${libraries.size} Open-Source Components • Tap to view license",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(libraries) { lib ->
                val isExpanded = expandedStates[lib.id] == true

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedStates[lib.id] = !isExpanded
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lib.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = lib.licenseType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "By ${lib.author}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = lib.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Hide Full License Text" else "View Embedded License Text",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = lib.fullLicenseText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("${lib.name} License", lib.fullLicenseText))
                                            Toast.makeText(context, "License text copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy License")
                                    }

                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lib.url)).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Website")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

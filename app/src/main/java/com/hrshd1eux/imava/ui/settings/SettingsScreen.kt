package com.hrshd1eux.imava.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.draw.shadow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appTheme = viewModel.appTheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {

        item {
            SettingsCategoryHeader(title = "Appearance & Theme", icon = Icons.Default.Palette)
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Choose App Theme", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    ThemeOptionRow(
                        title = "System Default",
                        selected = appTheme == "system",
                        onClick = { viewModel.appTheme = "system" }
                    )
                    ThemeOptionRow(
                        title = "Dark Theme",
                        selected = appTheme == "dark",
                        onClick = { viewModel.appTheme = "dark" }
                    )
                    ThemeOptionRow(
                        title = "AMOLED Dark (Pure Black)",
                        selected = appTheme == "amoled",
                        onClick = { viewModel.appTheme = "amoled" }
                    )
                    ThemeOptionRow(
                        title = "Light Theme",
                        selected = appTheme == "light",
                        onClick = { viewModel.appTheme = "light" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Default Startup Screen", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    val galleryPrefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }
                    var startScreen by remember { mutableStateOf(galleryPrefs.getString("default_start_screen", "timeline") ?: "timeline") }

                    ThemeOptionRow(
                        title = "Photos (Main Timeline)",
                        selected = startScreen == "timeline",
                        onClick = {
                            startScreen = "timeline"
                            galleryPrefs.edit().putString("default_start_screen", "timeline").apply()
                        }
                    )
                    ThemeOptionRow(
                        title = "Albums",
                        selected = startScreen == "albums",
                        onClick = {
                            startScreen = "albums"
                            galleryPrefs.edit().putString("default_start_screen", "albums").apply()
                        }
                    )
                    ThemeOptionRow(
                        title = "Remember Last Active Tab",
                        selected = startScreen == "last_active",
                        onClick = {
                            startScreen = "last_active"
                            galleryPrefs.edit().putString("default_start_screen", "last_active").apply()
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    var boostBrightness by remember { mutableStateOf(galleryPrefs.getBoolean("viewer_boost_brightness", false)) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Viewer Brightness Boost ☀️",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Automatically max out screen brightness when viewing photos and videos for maximum HDR detail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = boostBrightness,
                            onCheckedChange = { enabled ->
                                boostBrightness = enabled
                                galleryPrefs.edit().putBoolean("viewer_boost_brightness", enabled).apply()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }


        item {
            SettingsCategoryHeader(title = "Grid & Display Size", icon = Icons.Default.GridView)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val currentCols = viewModel.gridColumnCount
                    val label = when (currentCols) {
                        1 -> "1 Column (Extra Large)"
                        2 -> "2 Columns (Large)"
                        3 -> "3 Columns (Standard)"
                        4 -> "4 Columns (Compact)"
                        5 -> "5 Columns (Dense)"
                        6 -> "6 Columns (Overview)"
                        else -> "$currentCols Columns"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Timeline Columns",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adjust the default size of photos in the timeline grid. You can also pinch-to-zoom on the timeline to resize at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = currentCols.toFloat(),
                        onValueChange = { newValue ->
                            val cols = newValue.toInt().coerceIn(1, 6)
                            if (cols != currentCols) {
                                viewModel.setGridColumns(cols)
                                com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                            }
                        },
                        valueRange = 1f..6f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "1 (Big)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "3 (Normal)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "6 (Dense)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }


        item {
            val galleryPrefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }
            var hapticsEnabled by remember { mutableStateOf(galleryPrefs.getBoolean("haptics_enabled", true)) }

            SettingsCategoryHeader(title = "Touch & Haptics", icon = Icons.Default.Vibration)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Vibrations",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tactile feedback when selecting photos, unlocking vault, editing, and deleting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = { enabled ->
                            hapticsEnabled = enabled
                            galleryPrefs.edit().putBoolean("haptics_enabled", enabled).apply()
                            if (enabled) {
                                com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }


        item {
            val excludedSet by viewModel.excludedBucketIds.collectAsState()
            val allBuckets by viewModel.buckets.collectAsState()
            var showManageExcludedDialog by remember { mutableStateOf(false) }
            val autoScrubSet by viewModel.autoScrubBucketIds.collectAsState()
            var showManageAutoScrubDialog by remember { mutableStateOf(false) }

            // 1. Folders & Media Access Group
            SettingsCategoryHeader(title = "Folders & Media Access", icon = Icons.Default.Folder)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManageExcludedDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Excluded / Hidden Folders",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (excludedSet.isEmpty()) "No folders hidden" else "${excludedSet.size} folder(s) excluded from gallery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManageAutoScrubDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Scrub Albums",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (autoScrubSet.isEmpty()) "No albums configured" else "${autoScrubSet.size} album(s) automatically sanitized",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                val mergeSameNamed by viewModel.mergeSameNamedAlbums.collectAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Merge Same-Named Albums",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Combine folders with matching names (e.g. internal & SD Card Download) into one unified album card",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = mergeSameNamed,
                        onCheckedChange = { viewModel.setMergeSameNamedAlbums(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                val storagePrefs = remember(context) { context.getSharedPreferences("storage_prefs", Context.MODE_PRIVATE) }
                var sdTreeUri by remember { mutableStateOf(storagePrefs.getString("sd_tree_uri", null)) }

                val sdLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        try {
                            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            context.contentResolver.takePersistableUriPermission(uri, flags)
                            storagePrefs.edit().putString("sd_tree_uri", uri.toString()).apply()
                            sdTreeUri = uri.toString()
                            viewModel.refreshAll()
                            com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                            android.widget.Toast.makeText(context, "Storage permission granted", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sdLauncher.launch(null) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "External SD Card / USB OTG Access",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (sdTreeUri != null) "Permission granted for external storage" else "Grant Storage Access Framework tree permission for secondary storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.SdCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. App Lock & Device Privacy Group
            SettingsCategoryHeader(title = "App Lock & Device Privacy", icon = Icons.Default.Shield)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                var appLockEnabled by remember { mutableStateOf(com.hrshd1eux.imava.core.util.AppLockManager.isAppLockEnabled(context)) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Lock (Biometric / PIN) 🔒",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Require fingerprint, face, or device PIN every time Imava is opened",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = { enabled ->
                            com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
                                activity = context,
                                title = if (enabled) "Enable App Lock" else "Disable App Lock",
                                subtitle = if (enabled) "Verify your identity to lock the app" else "Verify your identity to disable app lock",
                                onSuccess = {
                                    com.hrshd1eux.imava.core.util.AppLockManager.setAppLockEnabled(context, enabled)
                                    appLockEnabled = enabled
                                    com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                },
                                onError = { err ->
                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                val vaultPrefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }
                var recentsPrivacyShield by remember {
                    mutableStateOf(vaultPrefs.getBoolean("recents_privacy_shield_enabled", true))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Switcher Privacy Shield 🛡️",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Blank out gallery thumbnails in the Android recent apps overview to prevent shoulder surfing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = recentsPrivacyShield,
                        onCheckedChange = { enabled ->
                            recentsPrivacyShield = enabled
                            vaultPrefs.edit().putBoolean("recents_privacy_shield_enabled", enabled).apply()
                            if (enabled) {
                                com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                            }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guest Hand-Off (App Pinning) 📌",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "When handing your phone to someone, tap the Pin 📌 icon in the photo viewer to lock the screen to that single item. Guests cannot swipe to adjacent media without biometric or PIN authentication.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Storage Cleaners & Maintenance Group
            SettingsCategoryHeader(title = "Storage Cleaners & Maintenance", icon = Icons.Default.Delete)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                val secureShredEnabled by viewModel.isSecureShredEnabled.collectAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cryptographic File Shredder 🛡️⚡",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Physically overwrite deleted photos with random data & zeroes to prevent recovery by forensic tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = secureShredEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setSecureShredEnabled(enabled)
                            if (enabled) {
                                com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                            }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                var isPurgingThumbnails by remember { mutableStateOf(false) }
                val purgeScope = rememberCoroutineScope()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ghost Thumbnail Purger 🧹⚡",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Shred orphaned unencrypted previews in DCIM/.thumbnails and cache to defeat forensic recovery tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (!isPurgingThumbnails) {
                                isPurgingThumbnails = true
                                purgeScope.launch {
                                    com.hrshd1eux.imava.core.util.HapticUtil.performClick(context)
                                    val result = com.hrshd1eux.imava.core.util.ThumbnailPurgeUtil.purgeAndShredGhostThumbnails(context)
                                    val mbFreed = String.format("%.2f MB", result.bytesReclaimed / (1024.0 * 1024.0))
                                    android.widget.Toast.makeText(
                                        context,
                                        "Shredded ${result.filesPurged} ghost preview files ($mbFreed reclaimed)!",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                    com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                    isPurgingThumbnails = false
                                }
                            }
                        },
                        enabled = !isPurgingThumbnails
                    ) {
                        if (isPurgingThumbnails) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Shred Now")
                        }
                    }
                }
            }

            if (showManageExcludedDialog) {
                AlertDialog(
                    onDismissRequest = { showManageExcludedDialog = false },
                    title = { Text("Excluded Folders") },
                    text = {
                        if (excludedSet.isEmpty()) {
                            Text("You have not excluded any folders yet. Long-press any album in the Albums tab to hide it.")
                        } else {
                            Column {
                                Text("These folders are hidden from your gallery view:")
                                Spacer(modifier = Modifier.height(8.dp))
                                excludedSet.toList().forEach { bucketId ->
                                    val folderName = allBuckets.find { it.id.toString() == bucketId }?.name ?: "Folder #$bucketId"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(folderName, style = MaterialTheme.typography.bodyMedium)
                                        TextButton(
                                            onClick = {
                                                viewModel.restoreExcludedBucket(bucketId)
                                            }
                                        ) {
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showManageExcludedDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            if (showManageAutoScrubDialog) {
                AlertDialog(
                    onDismissRequest = { showManageAutoScrubDialog = false },
                    title = { Text("Auto-Scrub Albums 🛡️") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = "Photos in selected albums are automatically sanitized in the background, removing GPS and camera fingerprints while preserving capture dates and orientation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (allBuckets.isEmpty()) {
                                Text("No albums discovered yet.")
                            } else {
                                allBuckets.forEach { bucket ->
                                    val isEnabled = autoScrubSet.contains(bucket.id.toString())
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleAutoScrubBucket(bucket.id) }
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bucket.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("${bucket.count} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Checkbox(
                                            checked = isEnabled,
                                            onCheckedChange = { viewModel.toggleAutoScrubBucket(bucket.id) }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showManageAutoScrubDialog = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }



        item {
            SettingsCategoryHeader(title = "Deep Media Scan & Storage", icon = Icons.Default.SdCard)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val isScanning by viewModel.isDeepScanning.collectAsState()
                    val lastScanCount by viewModel.lastDeepScanCount.collectAsState()
                    val lastScanNewCount by viewModel.lastDeepScanNewCount.collectAsState()
                    var autoScanEnabled by remember { mutableStateOf(viewModel.isAutoDeepScanEnabled) }

                    LaunchedEffect(Unit) {
                        viewModel.deepScanCompletionEvent.collect { result ->
                            val msg = if (result.newCount > 0) {
                                "Deep scan complete: ${result.newCount} new media items discovered (${result.totalCount} total indexed)"
                            } else {
                                "Deep scan complete: All ${result.totalCount} hidden media items are up to date"
                            }
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }

                    Text(
                        text = "Deep Media Discovery",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan sent media, private folders, GIFs, and other hidden .nomedia media across your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
                        !android.os.Environment.isExternalStorageManager()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "All Files Access Recommended",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Android 11+ restricts access to third-party app media folders. Grant All Files Access to discover all hidden media.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try {
                                                val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Could not open storage settings: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Grant Access")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Deep Scan",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (autoScanEnabled) "Auto-scan active on startup and media changes" else "Auto-scan disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoScanEnabled,
                            onCheckedChange = {
                                autoScanEnabled = it
                                viewModel.isAutoDeepScanEnabled = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (autoScanEnabled && lastScanCount > 0 && !isScanning) {
                        val statusText = if (lastScanNewCount > 0) {
                            "✓ Last scan: $lastScanNewCount new media items discovered ($lastScanCount total indexed)"
                        } else {
                            "✓ All $lastScanCount hidden media items are indexed and up to date"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            viewModel.triggerAutoDeepScan(force = true, isManual = true)
                            android.widget.Toast.makeText(context, "Deep scan started...", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning device...")
                        } else {
                            Text("Scan Now")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SettingsCategoryHeader(title = "About & Legal", icon = Icons.Default.Info)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        viewModel.currentScreen = com.hrshd1eux.imava.ui.Screen.AboutLegal
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "About & Legal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Developer, updates, licenses & permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCategoryHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

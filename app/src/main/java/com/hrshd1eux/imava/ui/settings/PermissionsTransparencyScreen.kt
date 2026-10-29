package com.hrshd1eux.imava.ui.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class PermissionDetail(
    val friendlyName: String,
    val technicalName: String,
    val category: String,
    val userFriendlyReason: String,
    val technicalExplanation: String,
    val introducedApi: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsTransparencyScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isTechnicalMode by remember { mutableStateOf(false) }

    // Known detailed explanations for permissions
    val detailedKnownPermissions = remember {
        mapOf(
            "android.permission.MANAGE_EXTERNAL_STORAGE" to PermissionDetail(
                friendlyName = "Full Device Storage Access",
                technicalName = "android.permission.MANAGE_EXTERNAL_STORAGE",
                category = "Storage & Files",
                userFriendlyReason = "Required on Android 11+ to browse, move, rename, delete, and organize photos in all folders (DCIM, Pictures, Downloads) without Scoped Storage restrictions.",
                technicalExplanation = "All Files Access (API 30+). Provides native filesystem access for album creation, lossless rotation, and batch renaming.",
                introducedApi = "API 30 (Android 11)"
            ),
            "android.permission.READ_MEDIA_IMAGES" to PermissionDetail(
                friendlyName = "Read Photo Library",
                technicalName = "android.permission.READ_MEDIA_IMAGES",
                category = "Media Access",
                userFriendlyReason = "Allows loading and viewing all photos on your device in the gallery timeline and albums.",
                technicalExplanation = "Granular Media Permission (API 33+). Required to query MediaStore.Images.Media table.",
                introducedApi = "API 33 (Android 13)"
            ),
            "android.permission.READ_MEDIA_VIDEO" to PermissionDetail(
                friendlyName = "Read Video Library",
                technicalName = "android.permission.READ_MEDIA_VIDEO",
                category = "Media Access",
                userFriendlyReason = "Allows displaying, streaming, and playing video files in the gallery video player.",
                technicalExplanation = "Granular Media Permission (API 33+). Required to query MediaStore.Video.Media table.",
                introducedApi = "API 33 (Android 13)"
            ),
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to PermissionDetail(
                friendlyName = "Partial Photo Selection",
                technicalName = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                category = "Media Access",
                userFriendlyReason = "Supports Android 14's partial photo picker if you choose to grant Imava access only to selected images.",
                technicalExplanation = "User-selected visual media permission (API 34+). Allows functioning seamlessly under restricted photo picker grants.",
                introducedApi = "API 34 (Android 14)"
            ),
            "android.permission.ACCESS_MEDIA_LOCATION" to PermissionDetail(
                friendlyName = "Photo Location & EXIF Geotags",
                technicalName = "android.permission.ACCESS_MEDIA_LOCATION",
                category = "Privacy & Location",
                userFriendlyReason = "Allows reading GPS coordinates embedded in your photos so you can view geotags in the photo information sheet.",
                technicalExplanation = "Runtime permission required on Android 10+ (API 29+) to access unredacted GPS EXIF metadata in MediaStore.",
                introducedApi = "API 29 (Android 10)"
            ),
            "android.permission.USE_BIOMETRIC" to PermissionDetail(
                friendlyName = "Biometric Authentication",
                technicalName = "android.permission.USE_BIOMETRIC",
                category = "Security & Vault",
                userFriendlyReason = "Allows using your device's fingerprint or face unlock to access your Private Vault and locked albums.",
                technicalExplanation = "AndroidX BiometricPrompt API wrapper for hardware-backed KeyStore authentication.",
                introducedApi = "API 28 (Android 9)"
            ),
            "android.permission.USE_FINGERPRINT" to PermissionDetail(
                friendlyName = "Legacy Fingerprint Support",
                technicalName = "android.permission.USE_FINGERPRINT",
                category = "Security & Vault",
                userFriendlyReason = "Backward-compatibility fallback for unlocking the vault via fingerprint on older devices.",
                technicalExplanation = "Deprecated fingerprint hardware API fallback for Android 9 and lower.",
                introducedApi = "API 23 (Android 6)"
            ),
            "android.permission.INTERNET" to PermissionDetail(
                friendlyName = "Internet Access",
                technicalName = "android.permission.INTERNET",
                category = "Network & Updates",
                userFriendlyReason = "Used solely to check GitHub Releases for app updates. Imava has zero analytics, zero crash loggers, and zero telemetry.",
                technicalExplanation = "Normal permission for network socket creation. Used only by AppUpdateManager for GitHub Releases API.",
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.ACCESS_NETWORK_STATE" to PermissionDetail(
                friendlyName = "Network State Check",
                technicalName = "android.permission.ACCESS_NETWORK_STATE",
                category = "Network & Updates",
                userFriendlyReason = "Checks if your device is connected to Wi-Fi or mobile data before checking GitHub for updates.",
                technicalExplanation = "Normal permission used by ConnectivityManager to prevent failed network requests when offline.",
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.ACCESS_LOCAL_NETWORK" to PermissionDetail(
                friendlyName = "Local Network Discovery",
                technicalName = "android.permission.ACCESS_LOCAL_NETWORK",
                category = "Network & Cast",
                userFriendlyReason = "Permits casting or discovering local devices on your home Wi-Fi network.",
                technicalExplanation = "Local network communication permission for LAN discovery protocols.",
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.FOREGROUND_SERVICE" to PermissionDetail(
                friendlyName = "Foreground Background Processing",
                technicalName = "android.permission.FOREGROUND_SERVICE",
                category = "System & Services",
                userFriendlyReason = "Keeps long-running batch operations (like shredding ghost thumbnails or bulk photo compression) active without the system killing the process.",
                technicalExplanation = "Required to run Android Foreground Services with a visible notification indicator.",
                introducedApi = "API 28 (Android 9)"
            ),
            "android.permission.WAKE_LOCK" to PermissionDetail(
                friendlyName = "Prevent Sleep During Tasks",
                technicalName = "android.permission.WAKE_LOCK",
                category = "System & Services",
                userFriendlyReason = "Prevents the device processor from sleeping during active slideshow playback, video playback, or bulk media export.",
                technicalExplanation = "PowerManager.WakeLock primitive used by WorkManager and ExoPlayer during active processing.",
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.VIBRATE" to PermissionDetail(
                friendlyName = "Haptic Vibration Feedback",
                technicalName = "android.permission.VIBRATE",
                category = "User Interface",
                userFriendlyReason = "Provides tactile haptic clicks when selecting photos, unlocking the vault, or dragging items.",
                technicalExplanation = "Vibrator / CombinedVibrator system service invocation.",
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.SET_WALLPAPER" to PermissionDetail(
                friendlyName = "Set as Device Wallpaper",
                technicalName = "android.permission.SET_WALLPAPER",
                category = "Customization",
                userFriendlyReason = "Allows applying any photo in your gallery directly as your Home or Lock screen wallpaper.",
                technicalExplanation = "WallpaperManager system service invocation.",
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.SET_WALLPAPER_HINTS" to PermissionDetail(
                friendlyName = "Wallpaper Alignment & Hints",
                technicalName = "android.permission.SET_WALLPAPER_HINTS",
                category = "Customization",
                userFriendlyReason = "Ensures wallpaper cropping and dimensions fit your screen correctly.",
                technicalExplanation = "WallpaperManager crop hints configuration.",
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.RECEIVE_BOOT_COMPLETED" to PermissionDetail(
                friendlyName = "Start Scheduled Tasks on Boot",
                technicalName = "android.permission.RECEIVE_BOOT_COMPLETED",
                category = "System & Services",
                userFriendlyReason = "Re-schedules background periodic maintenance (such as trash auto-cleanup and update checks) after your phone restarts.",
                technicalExplanation = "BroadcastReceiver for android.intent.action.BOOT_COMPLETED used by AndroidX WorkManager.",
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.REQUEST_INSTALL_PACKAGES" to PermissionDetail(
                friendlyName = "Direct App Update Installation",
                technicalName = "android.permission.REQUEST_INSTALL_PACKAGES",
                category = "Network & Updates",
                userFriendlyReason = "Allows installing updated versions of Imava downloaded from official GitHub Releases without requiring manual file browsing.",
                technicalExplanation = "Package installer intent invocation for user-approved in-place APK updates.",
                introducedApi = "API 26 (Android 8)"
            )
        )
    }

    // Dynamically retrieve all requested permissions declared in the manifest
    val dynamicPermissions = remember(context) {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            }

            val declared = pInfo.requestedPermissions ?: emptyArray()

            declared.map { permName ->
                detailedKnownPermissions[permName] ?: PermissionDetail(
                    friendlyName = permName.substringAfterLast("."),
                    technicalName = permName,
                    category = "System Permission",
                    userFriendlyReason = "Declared by Android system or supporting library.",
                    technicalExplanation = "Registered in AndroidManifest.xml: $permName",
                    introducedApi = "Standard Android API"
                )
            }
        } catch (_: Exception) {
            detailedKnownPermissions.values.toList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Permission Transparency",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${dynamicPermissions.size} Permissions Declared",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mode Selector Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Surface(
                        onClick = { isTechnicalMode = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isTechnicalMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (!isTechnicalMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simple (User Friendly)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (!isTechnicalMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { isTechnicalMode = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isTechnicalMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = if (isTechnicalMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Technical (Developer)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isTechnicalMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Permissions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(dynamicPermissions) { perm ->
                    val isGranted = remember(perm.technicalName) {
                        ContextCompat.checkSelfPermission(context, perm.technicalName) == PackageManager.PERMISSION_GRANTED ||
                                (perm.technicalName == "android.permission.MANAGE_EXTERNAL_STORAGE" &&
                                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
                                        android.os.Environment.isExternalStorageManager())
                    }

                    OutlinedCard(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Header: Name and Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isTechnicalMode) perm.technicalName.substringAfterLast(".") else perm.friendlyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isGranted) "Granted" else "Not Granted",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = perm.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isTechnicalMode) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = perm.technicalName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = perm.technicalExplanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Target Scope: ${perm.introducedApi}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                Text(
                                    text = perm.userFriendlyReason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
}

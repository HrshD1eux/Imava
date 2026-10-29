package com.hrshd1eux.imava.ui.settings

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class PermissionDetail(
    val friendlyName: String,
    val technicalName: String,
    val category: String,
    val userFriendlyReason: String,
    val technicalExplanation: String,
    val introducedApi: String
)

@Composable
fun PermissionsTransparencyDialog(
    onDismiss: () -> Unit
) {
    var isTechnicalMode by remember { mutableStateOf(false) }

    val permissionsList = remember {
        listOf(
            PermissionDetail(
                friendlyName = "Full Device Storage Access",
                technicalName = "android.permission.MANAGE_EXTERNAL_STORAGE",
                category = "Storage & Files",
                userFriendlyReason = "Required on Android 11+ to browse, move, rename, delete, and organize photos in all folders (DCIM, Pictures, Downloads) without Android Scoped Storage blocking folder operations.",
                technicalExplanation = "All Files Access (API 30+). Used by File, DocumentFile, and MediaStore DataSource to provide native filesystem access for album creation, lossless rotation, and batch renaming.",
                introducedApi = "API 30 (Android 11)"
            ),
            PermissionDetail(
                friendlyName = "Read Photo Library",
                technicalName = "android.permission.READ_MEDIA_IMAGES",
                category = "Media Access",
                userFriendlyReason = "Allows loading and viewing all photos on your device in the gallery timeline and albums.",
                technicalExplanation = "Granular Media Permission (API 33+). Required to query MediaStore.Images.Media table.",
                introducedApi = "API 33 (Android 13)"
            ),
            PermissionDetail(
                friendlyName = "Read Video Library",
                technicalName = "android.permission.READ_MEDIA_VIDEO",
                category = "Media Access",
                userFriendlyReason = "Allows displaying, streaming, and playing video files in the gallery video player.",
                technicalExplanation = "Granular Media Permission (API 33+). Required to query MediaStore.Video.Media table.",
                introducedApi = "API 33 (Android 13)"
            ),
            PermissionDetail(
                friendlyName = "Partial Photo Selection",
                technicalName = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                category = "Media Access",
                userFriendlyReason = "Supports Android 14's partial photo picker if you choose to grant Imava access only to selected images.",
                technicalExplanation = "User-selected visual media permission (API 34+). Allows functioning seamlessly under restricted photo picker grants.",
                introducedApi = "API 34 (Android 14)"
            ),
            PermissionDetail(
                friendlyName = "Photo Location & EXIF Geotags",
                technicalName = "android.permission.ACCESS_MEDIA_LOCATION",
                category = "Privacy & Location",
                userFriendlyReason = "Allows reading GPS coordinates from photo metadata so you can see where a picture was taken, or sanitize/strip GPS data before sharing.",
                technicalExplanation = "Redacted EXIF access (API 29+). Required by MediaStore.setRequireOriginal(uri) to read GPS latitude, longitude, and altitude from image EXIF headers.",
                introducedApi = "API 29 (Android 10)"
            ),
            PermissionDetail(
                friendlyName = "Biometric Lock (Fingerprint / Face)",
                technicalName = "android.permission.USE_BIOMETRIC",
                category = "Security & Vault",
                userFriendlyReason = "Allows unlocking your Private Vault and App Lock using your device's fingerprint or biometric sensor.",
                technicalExplanation = "Standard Android BiometricPrompt framework for hardware-backed local cryptographic authentication.",
                introducedApi = "API 28 (Android 9)"
            ),
            PermissionDetail(
                friendlyName = "Legacy Fingerprint Hardware",
                technicalName = "android.permission.USE_FINGERPRINT",
                category = "Security & Vault",
                userFriendlyReason = "Provides backward compatibility for fingerprint unlock on older Android devices.",
                technicalExplanation = "Legacy FingerprintManager API (deprecated in API 28, retained for API 23-27 compatibility).",
                introducedApi = "API 23 (Android 6.0)"
            ),
            PermissionDetail(
                friendlyName = "Set Wallpaper",
                technicalName = "android.permission.SET_WALLPAPER",
                category = "Display & UI",
                userFriendlyReason = "Enables the 'Set as Wallpaper' feature to apply any photo directly to your Home or Lock screen.",
                technicalExplanation = "WallpaperManager.setBitmap() / setStream() to write wallpaper bitmaps to system display server.",
                introducedApi = "API 1"
            ),
            PermissionDetail(
                friendlyName = "Wallpaper Display Optimization",
                technicalName = "android.permission.SET_WALLPAPER_HINTS",
                category = "Display & UI",
                userFriendlyReason = "Adjusts wallpaper dimension hints to perfectly match your phone's screen resolution and aspect ratio.",
                technicalExplanation = "WallpaperManager.suggestDesiredDimensions() to optimize wallpaper scaling buffers.",
                introducedApi = "API 1"
            ),
            PermissionDetail(
                friendlyName = "Haptic Vibration Feedback",
                technicalName = "android.permission.VIBRATE",
                category = "System & Hardware",
                userFriendlyReason = "Provides tactile vibration feedback when long-pressing, selecting items, or unlocking the vault.",
                technicalExplanation = "Vibrator / VibrationEffect framework for system haptic confirmation pulses.",
                introducedApi = "API 1"
            ),
            PermissionDetail(
                friendlyName = "Network & GitHub Updates",
                technicalName = "android.permission.INTERNET",
                category = "Networking",
                userFriendlyReason = "Strictly used to check GitHub releases for updates and cast media to local Wi-Fi devices. Zero photos or metadata ever leave your device.",
                technicalExplanation = "Socket and HttpURLConnection for GitHub Releases API and local DLNA socket server. No analytics or tracking SDKs.",
                introducedApi = "API 1"
            ),
            PermissionDetail(
                friendlyName = "Network State Check",
                technicalName = "android.permission.ACCESS_NETWORK_STATE",
                category = "Networking",
                userFriendlyReason = "Checks whether Wi-Fi or mobile data is connected before attempting update checks to save battery.",
                technicalExplanation = "ConnectivityManager.getNetworkCapabilities() to inspect transport availability.",
                introducedApi = "API 1"
            ),
            PermissionDetail(
                friendlyName = "Local Network Discovery",
                technicalName = "android.permission.ACCESS_LOCAL_NETWORK",
                category = "Networking",
                userFriendlyReason = "Allows discovering local Wi-Fi casting receivers or media players on your local home network.",
                technicalExplanation = "Local subnet broadcast / NSD service discovery.",
                introducedApi = "API 1"
            ),
            PermissionDetail(
                friendlyName = "Background Operations Service",
                technicalName = "android.permission.FOREGROUND_SERVICE",
                category = "System & Background",
                userFriendlyReason = "Ensures background tasks like batch compression, video trimming, or secure shredding are not terminated by Android if you switch apps.",
                technicalExplanation = "ForegroundService execution to guarantee CPU execution during long-running I/O operations.",
                introducedApi = "API 28 (Android 9)"
            ),
            PermissionDetail(
                friendlyName = "Reboot Protection & Auto-Lock",
                technicalName = "android.permission.RECEIVE_BOOT_COMPLETED",
                category = "System & Background",
                userFriendlyReason = "Locks the Private Vault immediately when the phone boots up, ensuring zero unauthorized access.",
                technicalExplanation = "BroadcastReceiver for ACTION_BOOT_COMPLETED to re-initialize AppLockManager and schedule update worker.",
                introducedApi = "API 1"
            ),
            PermissionDetail(
                friendlyName = "Keep Screen On",
                technicalName = "android.permission.WAKE_LOCK",
                category = "System & Hardware",
                userFriendlyReason = "Keeps your screen awake while playing videos or watching photo slideshows without the screen turning off.",
                technicalExplanation = "PowerManager.WakeLock / WindowManager FLAG_KEEP_SCREEN_ON during media playback.",
                introducedApi = "API 1"
            ),
            PermissionDetail(
                friendlyName = "Direct APK Update Installer",
                technicalName = "android.permission.REQUEST_INSTALL_PACKAGES",
                category = "Updates & Installation",
                userFriendlyReason = "Allows prompting installation when you choose to download an updated version directly from GitHub Releases.",
                technicalExplanation = "PackageInstaller intent trigger. Note: Documented for removal if publishing to Google Play Store to satisfy DDA policies.",
                introducedApi = "API 26 (Android 8.0)"
            ),
            PermissionDetail(
                friendlyName = "Internal App Component Guard",
                technicalName = "com.hrshd1eux.imava.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                category = "Internal Security",
                userFriendlyReason = "An internal security barrier created by AndroidX to prevent malicious third-party apps from intercepting Imava's internal messages.",
                technicalExplanation = "Signature-level runtime protection for dynamic BroadcastReceivers enforced by Android 14+.",
                introducedApi = "AndroidX Core Security"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Permission Transparency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "18 Permissions • 100% Local / Zero Tracking",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Segmented Mode Switcher: Standard vs Tech
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isTechnicalMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isTechnicalMode = false }
                                .padding(vertical = 8.dp),
                            shadowElevation = if (!isTechnicalMode) 2.dp else 0.dp
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (!isTechnicalMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Standard View",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!isTechnicalMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isTechnicalMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTechnicalMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isTechnicalMode = true }
                                .padding(vertical = 8.dp),
                            shadowElevation = if (isTechnicalMode) 2.dp else 0.dp
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = if (isTechnicalMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Technical (Dev)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isTechnicalMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTechnicalMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(permissionsList) { perm ->
                        OutlinedCard(
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
                                        text = if (isTechnicalMode) perm.technicalName.substringAfterLast('.') else perm.friendlyName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    ) {
                                        Text(
                                            text = perm.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (isTechnicalMode) {
                                    Text(
                                        text = perm.technicalName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = perm.technicalExplanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Introduced: ${perm.introducedApi}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    Text(
                                        text = perm.userFriendlyReason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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

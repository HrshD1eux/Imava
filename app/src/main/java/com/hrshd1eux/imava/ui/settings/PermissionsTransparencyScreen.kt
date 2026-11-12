package com.hrshd1eux.imava.ui.settings

import android.content.pm.PackageManager
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hrshd1eux.imava.R

data class PermissionDetail(
    @androidx.annotation.StringRes val friendlyNameRes: Int? = null,
    val fallbackFriendlyName: String = "",
    val technicalName: String,
    @androidx.annotation.StringRes val categoryRes: Int,
    @androidx.annotation.StringRes val userFriendlyReasonRes: Int,
    @androidx.annotation.StringRes val technicalExplanationRes: Int? = null,
    val introducedApi: String = "",
    @androidx.annotation.StringRes val introducedApiRes: Int? = null
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
            "android.permission.READ_EXTERNAL_STORAGE" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_read_ext,
                technicalName = "android.permission.READ_EXTERNAL_STORAGE",
                categoryRes = R.string.perm_cat_media,
                userFriendlyReasonRes = R.string.perm_reason_read_ext,
                technicalExplanationRes = R.string.perm_tech_read_ext,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.WRITE_EXTERNAL_STORAGE" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_write_ext,
                technicalName = "android.permission.WRITE_EXTERNAL_STORAGE",
                categoryRes = R.string.perm_cat_media,
                userFriendlyReasonRes = R.string.perm_reason_write_ext,
                technicalExplanationRes = R.string.perm_tech_write_ext,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.MANAGE_EXTERNAL_STORAGE" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_manage_storage,
                technicalName = "android.permission.MANAGE_EXTERNAL_STORAGE",
                categoryRes = R.string.perm_cat_storage,
                userFriendlyReasonRes = R.string.perm_reason_manage_storage,
                technicalExplanationRes = R.string.perm_tech_manage_storage,
                introducedApi = "API 30 (Android 11)"
            ),
            "android.permission.READ_MEDIA_IMAGES" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_read_images,
                technicalName = "android.permission.READ_MEDIA_IMAGES",
                categoryRes = R.string.perm_cat_media,
                userFriendlyReasonRes = R.string.perm_reason_read_images,
                technicalExplanationRes = R.string.perm_tech_read_images,
                introducedApi = "API 33 (Android 13)"
            ),
            "android.permission.READ_MEDIA_VIDEO" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_read_video,
                technicalName = "android.permission.READ_MEDIA_VIDEO",
                categoryRes = R.string.perm_cat_media,
                userFriendlyReasonRes = R.string.perm_reason_read_video,
                technicalExplanationRes = R.string.perm_tech_read_video,
                introducedApi = "API 33 (Android 13)"
            ),
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_visual_selected,
                technicalName = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                categoryRes = R.string.perm_cat_media,
                userFriendlyReasonRes = R.string.perm_reason_visual_selected,
                technicalExplanationRes = R.string.perm_tech_visual_selected,
                introducedApi = "API 34 (Android 14)"
            ),
            "android.permission.ACCESS_MEDIA_LOCATION" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_media_location,
                technicalName = "android.permission.ACCESS_MEDIA_LOCATION",
                categoryRes = R.string.perm_cat_privacy,
                userFriendlyReasonRes = R.string.perm_reason_media_location,
                technicalExplanationRes = R.string.perm_tech_media_location,
                introducedApi = "API 29 (Android 10)"
            ),
            "android.permission.USE_BIOMETRIC" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_biometric,
                technicalName = "android.permission.USE_BIOMETRIC",
                categoryRes = R.string.perm_cat_security,
                userFriendlyReasonRes = R.string.perm_reason_biometric,
                technicalExplanationRes = R.string.perm_tech_biometric,
                introducedApi = "API 28 (Android 9)"
            ),
            "android.permission.USE_FINGERPRINT" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_fingerprint,
                technicalName = "android.permission.USE_FINGERPRINT",
                categoryRes = R.string.perm_cat_security,
                userFriendlyReasonRes = R.string.perm_reason_fingerprint,
                technicalExplanationRes = R.string.perm_tech_fingerprint,
                introducedApi = "API 23 (Android 6)"
            ),
            "android.permission.INTERNET" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_internet,
                technicalName = "android.permission.INTERNET",
                categoryRes = R.string.perm_cat_network,
                userFriendlyReasonRes = R.string.perm_reason_internet,
                technicalExplanationRes = R.string.perm_tech_internet,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.ACCESS_NETWORK_STATE" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_network_state,
                technicalName = "android.permission.ACCESS_NETWORK_STATE",
                categoryRes = R.string.perm_cat_network,
                userFriendlyReasonRes = R.string.perm_reason_network_state,
                technicalExplanationRes = R.string.perm_tech_network_state,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.ACCESS_WIFI_STATE" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_wifi_state,
                technicalName = "android.permission.ACCESS_WIFI_STATE",
                categoryRes = R.string.perm_cat_network,
                userFriendlyReasonRes = R.string.perm_reason_wifi_state,
                technicalExplanationRes = R.string.perm_tech_wifi_state,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.CHANGE_WIFI_MULTICAST_STATE" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_wifi_multicast,
                technicalName = "android.permission.CHANGE_WIFI_MULTICAST_STATE",
                categoryRes = R.string.perm_cat_network,
                userFriendlyReasonRes = R.string.perm_reason_wifi_multicast,
                technicalExplanationRes = R.string.perm_tech_wifi_multicast,
                introducedApi = "API 4 (Android 1.6)"
            ),
            "android.permission.ACCESS_LOCAL_NETWORK" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_local_network,
                technicalName = "android.permission.ACCESS_LOCAL_NETWORK",
                categoryRes = R.string.perm_cat_cast,
                userFriendlyReasonRes = R.string.perm_reason_local_network,
                technicalExplanationRes = R.string.perm_tech_local_network,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.FOREGROUND_SERVICE" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_fg_service,
                technicalName = "android.permission.FOREGROUND_SERVICE",
                categoryRes = R.string.perm_cat_system,
                userFriendlyReasonRes = R.string.perm_reason_fg_service,
                technicalExplanationRes = R.string.perm_tech_fg_service,
                introducedApi = "API 28 (Android 9)"
            ),
            "android.permission.FOREGROUND_SERVICE_DATA_SYNC" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_fg_data_sync,
                technicalName = "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
                categoryRes = R.string.perm_cat_system,
                userFriendlyReasonRes = R.string.perm_reason_fg_data_sync,
                technicalExplanationRes = R.string.perm_tech_fg_data_sync,
                introducedApi = "API 34 (Android 14)"
            ),
            "android.permission.POST_NOTIFICATIONS" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_notifications,
                technicalName = "android.permission.POST_NOTIFICATIONS",
                categoryRes = R.string.perm_cat_system,
                userFriendlyReasonRes = R.string.perm_reason_notifications,
                technicalExplanationRes = R.string.perm_tech_notifications,
                introducedApi = "API 33 (Android 13)"
            ),
            "android.permission.WAKE_LOCK" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_wake_lock,
                technicalName = "android.permission.WAKE_LOCK",
                categoryRes = R.string.perm_cat_system,
                userFriendlyReasonRes = R.string.perm_reason_wake_lock,
                technicalExplanationRes = R.string.perm_tech_wake_lock,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.VIBRATE" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_vibrate,
                technicalName = "android.permission.VIBRATE",
                categoryRes = R.string.perm_cat_ui,
                userFriendlyReasonRes = R.string.perm_reason_vibrate,
                technicalExplanationRes = R.string.perm_tech_vibrate,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.SET_WALLPAPER" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_wallpaper,
                technicalName = "android.permission.SET_WALLPAPER",
                categoryRes = R.string.perm_cat_customization,
                userFriendlyReasonRes = R.string.perm_reason_wallpaper,
                technicalExplanationRes = R.string.perm_tech_wallpaper,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.SET_WALLPAPER_HINTS" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_wallpaper_hints,
                technicalName = "android.permission.SET_WALLPAPER_HINTS",
                categoryRes = R.string.perm_cat_customization,
                userFriendlyReasonRes = R.string.perm_reason_wallpaper_hints,
                technicalExplanationRes = R.string.perm_tech_wallpaper_hints,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.RECEIVE_BOOT_COMPLETED" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_boot,
                technicalName = "android.permission.RECEIVE_BOOT_COMPLETED",
                categoryRes = R.string.perm_cat_system,
                userFriendlyReasonRes = R.string.perm_reason_boot,
                technicalExplanationRes = R.string.perm_tech_boot,
                introducedApi = "API 1 (Android 1)"
            ),
            "android.permission.REQUEST_INSTALL_PACKAGES" to PermissionDetail(
                friendlyNameRes = R.string.perm_name_install_apk,
                technicalName = "android.permission.REQUEST_INSTALL_PACKAGES",
                categoryRes = R.string.perm_cat_updates,
                userFriendlyReasonRes = R.string.perm_reason_install_apk,
                technicalExplanationRes = R.string.perm_tech_install_apk,
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
                    fallbackFriendlyName = permName.substringAfterLast("."),
                    technicalName = permName,
                    categoryRes = R.string.perm_cat_fallback,
                    userFriendlyReasonRes = R.string.perm_fallback_reason,
                    introducedApiRes = R.string.perm_fallback_api
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
                            text = stringResource(R.string.about_perm_transparency),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.perm_declared_count, dynamicPermissions.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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
                                text = stringResource(R.string.perm_mode_simple),
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
                                text = stringResource(R.string.perm_mode_technical),
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

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
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
                                    text = if (isTechnicalMode) perm.technicalName.substringAfterLast(".")
                                    else perm.friendlyNameRes?.let { stringResource(it) } ?: perm.fallbackFriendlyName,
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
                                            text = if (isGranted) stringResource(R.string.perm_status_granted) else stringResource(R.string.perm_status_not_granted),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(perm.categoryRes),
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
                                    text = perm.technicalExplanationRes?.let { stringResource(it) }
                                        ?: stringResource(R.string.perm_fallback_tech, perm.technicalName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                val apiLabel = perm.introducedApiRes?.let { stringResource(it) } ?: perm.introducedApi
                                Text(
                                    text = stringResource(R.string.perm_target_scope, apiLabel),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                Text(
                                    text = stringResource(perm.userFriendlyReasonRes),
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

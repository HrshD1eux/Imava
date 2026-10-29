package com.hrshd1eux.imava.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.sync.webdav.WebDavClient
import com.hrshd1eux.imava.data.sync.webdav.WebDavConfig
import com.hrshd1eux.imava.data.sync.webdav.WebDavConfigManager
import com.hrshd1eux.imava.data.sync.webdav.WebDavSyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WebDavSettingsDialog(
    allItems: List<MediaItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(WebDavConfigManager.loadConfig(context)) }
    var passwordVisible by remember { mutableStateOf(false) }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var isTestSuccessful by remember { mutableStateOf<Boolean?>(null) }

    val isSyncing by WebDavSyncManager.isSyncing.collectAsState()
    val syncStatusText by WebDavSyncManager.syncStatus.collectAsState()
    val syncFraction by WebDavSyncManager.syncProgressFraction.collectAsState()

    AlertDialog(
        onDismissRequest = {
            if (!isSyncing) onDismiss()
        },
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Local NAS / WebDAV Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isSyncing, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Direct, zero-cloud backup to your home Synology, Nextcloud, TrueNAS, QNAP, or personal WebDAV server over local Wi-Fi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Server URL
                OutlinedTextField(
                    value = config.serverUrl,
                    onValueChange = {
                        config = config.copy(serverUrl = it)
                        testStatusMessage = null
                        isTestSuccessful = null
                    },
                    label = { Text("WebDAV Server URL") },
                    placeholder = { Text("http://192.168.1.100:8080/webdav") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Username
                OutlinedTextField(
                    value = config.username,
                    onValueChange = {
                        config = config.copy(username = it)
                        testStatusMessage = null
                        isTestSuccessful = null
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password
                OutlinedTextField(
                    value = config.password,
                    onValueChange = {
                        config = config.copy(password = it)
                        testStatusMessage = null
                        isTestSuccessful = null
                    },
                    label = { Text("Password / App Token") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Remote directory
                OutlinedTextField(
                    value = config.remoteDirectory,
                    onValueChange = { config = config.copy(remoteDirectory = it) },
                    label = { Text("Remote Destination Folder") },
                    placeholder = { Text("Imava_Backup") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Test Connection Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            testStatusMessage = null
                            isTestSuccessful = null
                            scope.launch {
                                val result = WebDavClient.testConnection(config)
                                isTestingConnection = false
                                if (result.isSuccess) {
                                    isTestSuccessful = true
                                    testStatusMessage = result.getOrNull() ?: "Connected successfully"
                                } else {
                                    isTestSuccessful = false
                                    testStatusMessage = result.exceptionOrNull()?.message ?: "Connection failed"
                                }
                            }
                        },
                        enabled = config.isConfigured && !isTestingConnection
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...")
                        } else {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Connection")
                        }
                    }
                }

                if (testStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTestSuccessful == true) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isTestSuccessful == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isTestSuccessful == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = testStatusMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isTestSuccessful == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Sync Options
                Text(
                    text = "Backup Rules",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Wi-Fi only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wi-Fi Only", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Never use cellular mobile data for NAS backup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = config.syncWifiOnly, onCheckedChange = { config = config.copy(syncWifiOnly = it) })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // While charging only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("While Charging Only", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Sync only when plugged into a power source", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = config.syncChargingOnly, onCheckedChange = { config = config.copy(syncChargingOnly = it) })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Include videos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include Videos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Upload videos in addition to photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = config.includeVideos, onCheckedChange = { config = config.copy(includeVideos = it) })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Daily auto backup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Background Auto-Backup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Automatically back up new photos once daily", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.autoBackupEnabled,
                        onCheckedChange = {
                            config = config.copy(autoBackupEnabled = it)
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Manual Backup & Live Progress Section
                Text(
                    text = "Manual Backup",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                val syncedIdsCount = remember(isSyncing) { WebDavConfigManager.getSyncedIds(context).size }
                Text(
                    text = "Status: ${config.lastBackupStatus} ($syncedIdsCount items tracked)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (config.lastBackupTimestamp > 0L) {
                    val formattedTime = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(config.lastBackupTimestamp))
                    Text(
                        text = "Last sync: $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isSyncing) {
                    LinearProgressIndicator(
                        progress = { syncFraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = syncStatusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                WebDavConfigManager.saveConfig(context, config)
                                WebDavSyncManager.startSync(
                                    context = context,
                                    scope = scope,
                                    allItems = allItems
                                )
                            },
                            enabled = config.isConfigured,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back Up Now")
                        }

                        if (syncedIdsCount > 0) {
                            OutlinedButton(
                                onClick = {
                                    WebDavConfigManager.clearSyncedIds(context)
                                    com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset History", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    WebDavConfigManager.saveConfig(context, config)
                    WebDavSyncManager.scheduleAutoBackup(context, config)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save & Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSyncing
            ) {
                Text("Cancel")
            }
        }
    )
}

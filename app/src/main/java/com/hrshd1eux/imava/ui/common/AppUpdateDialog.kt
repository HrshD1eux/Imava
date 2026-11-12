package com.hrshd1eux.imava.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.AppUpdateDownloadState
import com.hrshd1eux.imava.core.util.AppUpdateManager
import com.hrshd1eux.imava.core.util.FormatUtils
import com.hrshd1eux.imava.core.util.UpdateInfo

@Composable
fun AppUpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isNotesExpanded by remember { mutableStateOf(false) }

    val downloadState by AppUpdateManager.downloadState.collectAsState()

    fun dismissAndMarkSeen() {
        AppUpdateManager.markUpdateDismissed(context, updateInfo.latestVersion)
        onDismiss()
    }

    // Active downloading or paused or completed state dialog
    when (val state = downloadState) {
        is AppUpdateDownloadState.Downloading -> {
            AlertDialog(
                onDismissRequest = { dismissAndMarkSeen() },
                shape = RoundedCornerShape(22.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.update_downloading_title, state.version))
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${state.progress}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val downloadedStr = FormatUtils.formatFileSize(state.downloadedBytes)
                            val totalStr = if (state.totalBytes > 0) FormatUtils.formatFileSize(state.totalBytes) else "..."
                            Text(
                                text = "$downloadedStr / $totalStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_downloading_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { dismissAndMarkSeen() }) {
                        Text(stringResource(R.string.update_run_background))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { AppUpdateManager.pauseDownload(context) }) {
                            Text(stringResource(R.string.update_pause))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = {
                            AppUpdateManager.cancelDownload(context)
                            dismissAndMarkSeen()
                        }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            )
            return
        }
        is AppUpdateDownloadState.Paused -> {
            AlertDialog(
                onDismissRequest = { dismissAndMarkSeen() },
                shape = RoundedCornerShape(22.dp),
                title = { Text(stringResource(R.string.update_paused_title, state.version)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val downloadedStr = FormatUtils.formatFileSize(state.downloadedBytes)
                        val totalStr = if (state.totalBytes > 0) FormatUtils.formatFileSize(state.totalBytes) else "..."
                        Text(
                            text = stringResource(R.string.update_paused_hint, state.progress, downloadedStr, totalStr),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { AppUpdateManager.resumeDownload(context) }) {
                        Text(stringResource(R.string.update_resume))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { dismissAndMarkSeen() }) {
                            Text(stringResource(R.string.update_background))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = {
                            AppUpdateManager.cancelDownload(context)
                            dismissAndMarkSeen()
                        }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            )
            return
        }
        is AppUpdateDownloadState.Completed -> {
            AlertDialog(
                onDismissRequest = { dismissAndMarkSeen() },
                shape = RoundedCornerShape(22.dp),
                title = { Text(stringResource(R.string.update_ready_title)) },
                text = {
                    Text(
                        text = stringResource(R.string.update_ready_desc, state.version),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        AppUpdateManager.installApk(context, state.apkFile)
                        dismissAndMarkSeen()
                    }) {
                        Text(stringResource(R.string.update_install_now))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dismissAndMarkSeen() }) {
                        Text(stringResource(R.string.update_later))
                    }
                }
            )
            return
        }
        is AppUpdateDownloadState.Failed -> {
            AlertDialog(
                onDismissRequest = { dismissAndMarkSeen() },
                shape = RoundedCornerShape(22.dp),
                title = { Text(stringResource(R.string.update_interrupted_title)) },
                text = {
                    val downloadedStr = FormatUtils.formatFileSize(state.downloadedBytes)
                    Text(
                        text = stringResource(R.string.update_failed_desc, state.error, downloadedStr),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val browserUrl = updateInfo.apkDownloadUrl ?: "https://github.com/HrshD1eux/imava/releases/latest"
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                                dismissAndMarkSeen()
                            }
                        ) {
                            Text(stringResource(R.string.update_download_browser))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { AppUpdateManager.resumeDownload(context) }) {
                            Text(stringResource(R.string.update_resume))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        AppUpdateManager.cancelDownload(context)
                        dismissAndMarkSeen()
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
            return
        }
        AppUpdateDownloadState.Idle -> { /* Show main update prompt below */ }
    }

    AlertDialog(
        onDismissRequest = { dismissAndMarkSeen() },
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { dismissAndMarkSeen() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Version badge box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.update_installed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "v${updateInfo.currentVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.update_latest),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "v${updateInfo.latestVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Collapsible What's New Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { isNotesExpanded = !isNotesExpanded }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.update_whats_new, updateInfo.latestVersion),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isNotesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isNotesExpanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = isNotesExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            val cleanNotes = remember(updateInfo.releaseNotes) {
                                AppUpdateManager.formatCleanReleaseNotes(updateInfo.releaseNotes)
                            }
                            Text(
                                text = cleanNotes.ifBlank { stringResource(R.string.update_default_notes) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val browserUrl = updateInfo.apkDownloadUrl ?: "https://github.com/HrshD1eux/imava/releases/latest"
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        dismissAndMarkSeen()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = stringResource(R.string.update_download_browser),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = {
                        AppUpdateManager.startOrResumeDownload(context, updateInfo)
                        dismissAndMarkSeen()
                    }
                ) {
                    Text(stringResource(R.string.update_background))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        AppUpdateManager.startOrResumeDownload(context, updateInfo)
                    }
                ) {
                    Text(stringResource(R.string.update_now))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { dismissAndMarkSeen() }) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}

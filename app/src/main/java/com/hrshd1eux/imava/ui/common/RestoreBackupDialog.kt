package com.hrshd1eux.imava.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.HapticUtil
import com.hrshd1eux.imava.core.util.MetadataAutoBackupManager

@Composable
fun RestoreBackupDialog(
    backupInfo: MetadataAutoBackupManager.BackupInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.SettingsBackupRestore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.restore_backup_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.restore_backup_desc,
                        backupInfo.formattedDate,
                        backupInfo.tagCount,
                        backupInfo.favoriteCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.restore_backup_subtext),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    HapticUtil.performSuccess(context)
                    onConfirm()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.restore))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    HapticUtil.performSelection(context)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.not_now))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

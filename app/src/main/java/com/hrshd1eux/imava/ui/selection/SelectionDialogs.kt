package com.hrshd1eux.imava.ui.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.SharePrivacyMode

@Composable
fun SelectionShareDialog(
    show: Boolean,
    isViewingVault: Boolean,
    onDismiss: () -> Unit,
    onConfirmShare: (SharePrivacyMode) -> Unit,
    onDropClicked: () -> Unit
) {
    if (!show) return

    var privacyMode by remember { mutableStateOf(SharePrivacyMode.STRIP_ALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.share_choose_privacy),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { privacyMode = SharePrivacyMode.STRIP_ALL }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = privacyMode == SharePrivacyMode.STRIP_ALL,
                        onClick = { privacyMode = SharePrivacyMode.STRIP_ALL }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.share_strip_all), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.share_strip_all_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { privacyMode = SharePrivacyMode.FUZZ_LOCATION }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = privacyMode == SharePrivacyMode.FUZZ_LOCATION,
                        onClick = { privacyMode = SharePrivacyMode.FUZZ_LOCATION }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.share_fuzz_gps), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.share_fuzz_gps_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { privacyMode = SharePrivacyMode.FULL_METADATA }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = privacyMode == SharePrivacyMode.FULL_METADATA,
                        onClick = { privacyMode = SharePrivacyMode.FULL_METADATA }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.share_full_metadata), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.share_full_metadata_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isViewingVault) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onDropClicked()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.share_via_wifi_drop_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.share_via_wifi_drop_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmShare(privacyMode) }) {
                Text(stringResource(R.string.action_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun VaultPreConfirmDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.vault_hide_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.vault_hide_dialog_message))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.vault_hide_allow_next_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onProceed) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun VaultSetupRequiredDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onSetup: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.vault_setup_required_title)) },
        text = {
            Text(stringResource(R.string.vault_setup_required_message))
        },
        confirmButton = {
            Button(onClick = onSetup) {
                Text(stringResource(R.string.vault_setup_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun SelectionRestoreConfirmDialog(
    show: Boolean,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.RestoreFromTrash, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.restore_items_confirm_title)) },
        text = { Text(stringResource(R.string.restore_items_confirm_message, count)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.action_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun SelectionDeleteConfirmDialog(
    show: Boolean,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trash_items_confirm_title)) },
        text = { Text(stringResource(R.string.trash_items_confirm_message, count)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.move_to_trash))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun SelectionDeletePermanentlyConfirmDialog(
    show: Boolean,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_permanently_confirm_title)) },
        text = { Text(stringResource(R.string.delete_permanently_confirm_message, count)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete_permanently))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

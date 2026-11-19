package com.hrshd1eux.imava.ui.vault

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.ui.common.AutoFitText
import com.hrshd1eux.imava.ui.MainViewModel
import com.hrshd1eux.imava.core.util.HapticUtil
import com.hrshd1eux.imava.core.util.VaultCrypto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeMasterPasswordDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    currentLockType: String,
    onLockTypeChanged: (String) -> Unit
) {
    if (!show) return

    var oldPassword by remember { mutableStateOf("") }
    var newLockType by remember { mutableStateOf(currentLockType) }
    var changeError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.vault_set_change_master_title), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                AutoFitText(
                                    text = stringResource(R.string.action_cancel),
                                    maxLines = 1,
                                    minFontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    // Validation and saving logic here
                                    onLockTypeChanged(newLockType)
                                    onDismiss()
                                    HapticUtil.performSuccess(context)
                                    Toast.makeText(context, context.getString(R.string.vault_set_master_updated_toast), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                AutoFitText(
                                    text = stringResource(R.string.action_save),
                                    maxLines = 1,
                                    minFontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.vault_set_verify_current), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; changeError = null },
                        label = { Text(stringResource(R.string.vault_set_current_pass_pin)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                    Text(stringResource(R.string.vault_set_choose_new_type), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // ... [RadioButtons for lock type]
                }
            }
        }
    }
}

@Composable
fun SetDecoyCredentialDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    lockType: String,
    prefs: android.content.SharedPreferences,
    hasDecoyPin: Boolean,
    onHasDecoyPinChanged: (Boolean) -> Unit
) {
    if (!show) return

    var decoyInput by remember { mutableStateOf("") }
    var confirmDecoyInput by remember { mutableStateOf("") }
    var decoyError by remember { mutableStateOf<String?>(null) }
    var decoyPatternStep by remember { mutableIntStateOf(1) }
    var decoyPatternFirst by remember { mutableStateOf("") }
    var decoyPatternResult by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val dialogTitle = when (lockType) {
        "PATTERN" -> stringResource(R.string.vault_set_decoy_pattern_title)
        "PASSWORD" -> stringResource(R.string.vault_set_decoy_pass_title)
        else -> stringResource(R.string.vault_set_decoy_title)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasDecoyPin) {
                                TextButton(
                                    onClick = {
                                        prefs.edit()
                                            .remove("vault_decoy_pin_hash")
                                            .remove("vault_decoy_salt")
                                            .remove("vault_decoy_lock_type")
                                            .apply()
                                        onHasDecoyPinChanged(false)
                                        onDismiss()
                                        Toast.makeText(context, context.getString(R.string.vault_decoy_removed_toast), Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    AutoFitText(
                                        text = stringResource(R.string.vault_set_remove_decoy_btn),
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        minFontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            TextButton(onClick = onDismiss) {
                                AutoFitText(
                                    text = stringResource(R.string.action_cancel),
                                    maxLines = 1,
                                    minFontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val codeToSave = if (lockType == "PATTERN") decoyPatternResult else decoyInput.trim()
                                    // Validation logic (simplified)
                                    val decoySalt = VaultCrypto.generateSalt()
                                    val hash = VaultCrypto.hashPin(codeToSave, decoySalt)
                                    val encHash = VaultCrypto.encryptString(hash)
                                    val decoySaltB64 = java.util.Base64.getEncoder().encodeToString(decoySalt)
                                    prefs.edit()
                                        .putString("vault_decoy_pin_hash", encHash)
                                        .putString("vault_decoy_salt", decoySaltB64)
                                        .putString("vault_decoy_lock_type", lockType)
                                        .apply()
                                    onHasDecoyPinChanged(true)
                                    onDismiss()
                                    HapticUtil.performSuccess(context)
                                    Toast.makeText(context, context.getString(R.string.vault_set_decoy_activated_toast), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                AutoFitText(
                                    text = stringResource(R.string.vault_set_save_decoy_btn),
                                    maxLines = 1,
                                    minFontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (lockType == "PATTERN") {
                        Text(
                            text = if (decoyPatternStep == 1) stringResource(R.string.vault_set_draw_decoy_pattern_hint) else stringResource(R.string.vault_set_confirm_decoy_pattern_hint),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PatternLockView(
                            modifier = Modifier.size(280.dp),
                            onPatternComplete = { p ->
                                if (p.split("-").size < 4) {
                                    decoyError = context.getString(R.string.vault_set_min_4_dots)
                                } else if (decoyPatternStep == 1) {
                                    decoyPatternFirst = p
                                    decoyPatternStep = 2
                                    decoyError = null
                                } else {
                                    if (p == decoyPatternFirst) {
                                        decoyPatternResult = p
                                        decoyError = null
                                    } else {
                                        decoyError = context.getString(R.string.vault_set_patterns_mismatch)
                                        decoyPatternStep = 1
                                    }
                                }
                            }
                        )
                    } else {
                        val isPin = lockType == "PIN"
                        OutlinedTextField(
                            value = decoyInput,
                            onValueChange = { decoyInput = it; decoyError = null },
                            label = { Text(if (isPin) stringResource(R.string.vault_decoy_pin_label) else stringResource(R.string.vault_set_decoy_pass_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmDecoyInput,
                            onValueChange = { confirmDecoyInput = it; decoyError = null },
                            label = { Text(if (isPin) stringResource(R.string.vault_decoy_pin_confirm_label) else stringResource(R.string.vault_set_decoy_pass_confirm_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (decoyError != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(decoyError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }

            }
        }
    }
}


@Composable
fun DeleteVaultConfirmationDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    context: android.content.Context,
    viewModel: com.hrshd1eux.imava.ui.MainViewModel,
    onVaultDeleted: () -> Unit
) {
    if (!show) return
    var restoreMediaOption by remember { mutableStateOf(true) }
    var isProcessingDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessingDelete) onDismiss() },
        title = { Text(stringResource(R.string.vault_set_delete_confirm_title)) },
        text = {
            Column {
                Text(stringResource(R.string.vault_set_delete_choose))
                // ... [Radio buttons for restore vs shred]
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessingDelete = true
                    viewModel.deleteVault(context, restoreMedia = restoreMediaOption) {
                        isProcessingDelete = false
                        onDismiss()
                        HapticUtil.performSuccess(context)
                        onVaultDeleted()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = !isProcessingDelete
            ) {
                if (isProcessingDelete) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text(if (restoreMediaOption) stringResource(R.string.vault_set_restore_delete_btn) else stringResource(R.string.vault_set_perm_shred_btn))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isProcessingDelete) { Text(stringResource(R.string.action_cancel)) } }
    )
}


@Composable
fun ExportPasswordDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onExportConfirmed: (String) -> Unit
) {
    if (!show) return
    var exportPassword by remember { mutableStateOf("") }
    var isProcessingBackup by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessingBackup) onDismiss() },
        title = { Text(stringResource(R.string.vault_set_backup_pass_title)) },
        text = {
            OutlinedTextField(
                value = exportPassword,
                onValueChange = { exportPassword = it },
                label = { Text(stringResource(R.string.vault_set_passphrase_label)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessingBackup = true
                    onExportConfirmed(exportPassword)
                    // Caller handles logic, we just trigger
                },
                enabled = exportPassword.isNotBlank() && !isProcessingBackup
            ) {
                if (isProcessingBackup) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text(stringResource(R.string.vault_export_btn))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isProcessingBackup) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun RestorePasswordDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onRestoreConfirmed: (String) -> Unit
) {
    if (!show) return
    var restorePassword by remember { mutableStateOf("") }
    var isProcessingBackup by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessingBackup) onDismiss() },
        title = { Text(stringResource(R.string.vault_set_enter_backup_pass_title)) },
        text = {
            OutlinedTextField(
                value = restorePassword,
                onValueChange = { restorePassword = it },
                label = { Text(stringResource(R.string.vault_set_passphrase_label)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessingBackup = true
                    onRestoreConfirmed(restorePassword)
                },
                enabled = restorePassword.isNotBlank() && !isProcessingBackup
            ) {
                if (isProcessingBackup) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text(stringResource(R.string.action_restore))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isProcessingBackup) { Text(stringResource(R.string.action_cancel)) } }
    )
}


package com.hrshd1eux.imava.ui.vault

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.HapticUtil
import com.hrshd1eux.imava.core.util.RootDetectionUtil
import com.hrshd1eux.imava.core.util.VaultBackupManager
import com.hrshd1eux.imava.core.util.VaultCrypto
import com.hrshd1eux.imava.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onVaultDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE) }

    var lockType by remember { mutableStateOf(prefs.getString("vault_lock_type", "PASSWORD") ?: "PASSWORD") }
    var isBiometricEnabled by remember { mutableStateOf(prefs.getBoolean("vault_biometric_enabled", false)) }
    var isStealthMode by remember { mutableStateOf(prefs.getBoolean("vault_stealth_mode", false)) }
    var secretTrigger by remember { mutableStateOf(prefs.getString("vault_secret_trigger", "#vault") ?: "#vault") }

    val galleryPrefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }
    var shakeLockEnabled by remember { mutableStateOf(galleryPrefs.getBoolean("vault_shake_lock_enabled", true)) }

    var hasDecoyPin by remember { mutableStateOf(prefs.getString("vault_decoy_pin_hash", null) != null) }
    var showDecoyPinDialog by remember { mutableStateOf(false) }

    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Backup & Restore state
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            pendingExportUri = uri
            showExportPasswordDialog = true
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestorePasswordDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.vault_set_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isRooted = remember { RootDetectionUtil.isDeviceRooted() }
            if (isRooted) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.vault_set_root_banner),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Card 1: Master Authentication
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.vault_set_master_auth),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vault_set_current_lock_type),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (lockType) {
                                    "PASSWORD" -> stringResource(R.string.vault_set_type_aes256)
                                    "PIN" -> stringResource(R.string.vault_dlg_numeric_pin)
                                    "PATTERN" -> stringResource(R.string.vault_set_type_pattern)
                                    else -> stringResource(R.string.vault_set_type_protected)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showPasswordChangeDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        com.hrshd1eux.imava.ui.common.AutoFitText(
                            text = stringResource(R.string.vault_set_change_master_btn),
                            maxLines = 1,
                            minFontSize = 11.sp
                        )
                    }
                }
            }

            // Card 2: Quick Access & Device Security
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.vault_set_quick_access),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vault_biometric_quick_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.vault_set_bio_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                isBiometricEnabled = enabled
                                prefs.edit().putBoolean("vault_biometric_enabled", enabled).apply()
                                if (enabled) HapticUtil.performSuccess(context)
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vault_set_shake_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.vault_set_shake_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = shakeLockEnabled,
                            onCheckedChange = { enabled ->
                                shakeLockEnabled = enabled
                                galleryPrefs.edit().putBoolean("vault_shake_lock_enabled", enabled).apply()
                                if (enabled) HapticUtil.performSuccess(context)
                            }
                        )
                    }
                }
            }

            // Card 3: Duress Protection (Decoy Vault)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val duressTitle = when (lockType) {
                        "PATTERN" -> stringResource(R.string.vault_set_duress_pattern_title)
                        "PASSWORD" -> stringResource(R.string.vault_set_duress_pass_title)
                        else -> stringResource(R.string.vault_set_duress_title)
                    }
                    val duressDesc = when (lockType) {
                        "PATTERN" -> stringResource(R.string.vault_set_duress_pattern_desc)
                        "PASSWORD" -> stringResource(R.string.vault_set_duress_pass_desc)
                        else -> stringResource(R.string.vault_set_duress_desc)
                    }
                    val decoyStatusText = if (hasDecoyPin) {
                        when (lockType) {
                            "PATTERN" -> stringResource(R.string.vault_set_decoy_pattern_active)
                            "PASSWORD" -> stringResource(R.string.vault_set_decoy_pass_active)
                            else -> stringResource(R.string.vault_set_decoy_active)
                        }
                    } else {
                        when (lockType) {
                            "PATTERN" -> stringResource(R.string.vault_set_decoy_pattern_unset)
                            "PASSWORD" -> stringResource(R.string.vault_set_decoy_pass_unset)
                            else -> stringResource(R.string.vault_set_decoy_unset)
                        }
                    }
                    val decoyBtnText = if (hasDecoyPin) {
                        when (lockType) {
                            "PATTERN" -> stringResource(R.string.vault_set_modify_decoy_pattern_btn)
                            "PASSWORD" -> stringResource(R.string.vault_set_modify_decoy_pass_btn)
                            else -> stringResource(R.string.vault_set_modify_decoy_btn)
                        }
                    } else {
                        when (lockType) {
                            "PATTERN" -> stringResource(R.string.vault_set_set_decoy_pattern_btn)
                            "PASSWORD" -> stringResource(R.string.vault_set_set_decoy_pass_btn)
                            else -> stringResource(R.string.vault_set_set_decoy_btn)
                        }
                    }

                    Text(
                        text = duressTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = duressDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.hrshd1eux.imava.ui.common.AutoFitText(
                            text = decoyStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (hasDecoyPin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                            maxLines = 1,
                            minFontSize = 11.sp
                        )

                        OutlinedButton(
                            onClick = { showDecoyPinDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            com.hrshd1eux.imava.ui.common.AutoFitText(
                                text = decoyBtnText,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                minFontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Card 4: Stealth Mode
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.vault_set_stealth_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vault_set_hide_albums_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.vault_set_hide_albums_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isStealthMode,
                            onCheckedChange = { enabled ->
                                isStealthMode = enabled
                                prefs.edit().putBoolean("vault_stealth_mode", enabled).apply()
                                if (enabled) HapticUtil.performSuccess(context)
                            }
                        )
                    }

                    if (isStealthMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = secretTrigger,
                            onValueChange = {
                                secretTrigger = it
                                prefs.edit().putString("vault_secret_trigger", it).apply()
                            },
                            label = { Text(stringResource(R.string.vault_set_secret_trigger_label)) },
                            placeholder = { Text(stringResource(R.string.vault_set_secret_trigger_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.vault_set_secret_trigger_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Card 5: Encrypted Backup & Restore
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.vault_set_backup_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.vault_set_backup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                    if (screenWidth < 360) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                    createDocLauncher.launch("Imava_Vault_Backup_$timestamp.imava")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                com.hrshd1eux.imava.ui.common.AutoFitText(
                                    text = stringResource(R.string.vault_set_export_btn),
                                    maxLines = 1,
                                    minFontSize = 11.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    openDocLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                com.hrshd1eux.imava.ui.common.AutoFitText(
                                    text = stringResource(R.string.vault_set_import_btn),
                                    maxLines = 1,
                                    minFontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                    createDocLauncher.launch("Imava_Vault_Backup_$timestamp.imava")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                com.hrshd1eux.imava.ui.common.AutoFitText(
                                    text = stringResource(R.string.vault_set_export_btn),
                                    maxLines = 1,
                                    minFontSize = 10.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    openDocLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                com.hrshd1eux.imava.ui.common.AutoFitText(
                                    text = stringResource(R.string.vault_set_import_btn),
                                    maxLines = 1,
                                    minFontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Card 6: Danger Zone / Vault Deletion
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.vault_set_destruction_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.vault_set_destruction_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        com.hrshd1eux.imava.ui.common.AutoFitText(
                            text = stringResource(R.string.vault_set_delete_ellipsis_btn),
                            maxLines = 1,
                            minFontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    ChangeMasterPasswordDialog(
        show = showPasswordChangeDialog,
        onDismiss = { showPasswordChangeDialog = false },
        currentLockType = lockType,
        onLockTypeChanged = { lockType = it }
    )

    SetDecoyCredentialDialog(
        show = showDecoyPinDialog,
        onDismiss = { showDecoyPinDialog = false },
        lockType = lockType,
        prefs = prefs,
        hasDecoyPin = hasDecoyPin,
        onHasDecoyPinChanged = { hasDecoyPin = it }
    )

    DeleteVaultConfirmationDialog(
        show = showDeleteConfirmDialog,
        onDismiss = { showDeleteConfirmDialog = false },
        context = context,
        viewModel = viewModel,
        onVaultDeleted = onVaultDeleted
    )

    ExportPasswordDialog(
        show = showExportPasswordDialog,
        onDismiss = {
            showExportPasswordDialog = false
            pendingExportUri = null
        },
        onExportConfirmed = { exportPassword ->
            val uri = pendingExportUri ?: return@ExportPasswordDialog
            scope.launch {
                try {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val db = com.hrshd1eux.imava.data.database.GalleryDatabase.getInstance(context)
                        val result = VaultBackupManager.exportVaultBackup(
                            context = context,
                            database = db,
                            targetOutputStream = outputStream,
                            passphrase = exportPassword.toCharArray()
                        )
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        if (result.success) {
                            HapticUtil.performSuccess(context)
                            showExportPasswordDialog = false
                            pendingExportUri = null
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.vault_set_err_open_write), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.vault_backup_export_failed_fmt, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    RestorePasswordDialog(
        show = showRestorePasswordDialog,
        onDismiss = {
            showRestorePasswordDialog = false
            pendingRestoreUri = null
        },
        onRestoreConfirmed = { restorePassword ->
            val uri = pendingRestoreUri ?: return@RestorePasswordDialog
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val db = com.hrshd1eux.imava.data.database.GalleryDatabase.getInstance(context)
                        val result = VaultBackupManager.restoreVaultBackup(
                            context = context,
                            database = db,
                            inputStream = inputStream,
                            passphrase = restorePassword.toCharArray()
                        )
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        if (result.success) {
                            HapticUtil.performSuccess(context)
                            viewModel.refreshAll()
                            showRestorePasswordDialog = false
                            pendingRestoreUri = null
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.vault_set_err_open_read), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.vault_set_restore_failed_fmt, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    )
}

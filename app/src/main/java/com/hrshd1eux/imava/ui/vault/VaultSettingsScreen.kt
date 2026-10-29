package com.hrshd1eux.imava.ui.vault

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
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
    val prefs = remember(context) { context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

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
    var isProcessingDelete by remember { mutableStateOf(false) }

    // Backup & Restore state
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var exportPassword by remember { mutableStateOf("") }

    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var isProcessingBackup by remember { mutableStateOf(false) }

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
                        text = "Vault Security & Encryption",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isRooted = remember { RootDetectionUtil.isDeviceRooted() }
            if (isRooted) {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Root detected: Superuser processes could potentially inspect device memory. Imava hardware AES-256 key protects files at rest.",
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
                        text = "Master Authentication",
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
                                text = "Current Lock Type",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (lockType) {
                                    "PASSWORD" -> "Alphanumeric Master Password (AES-256)"
                                    "PIN" -> "Numeric PIN"
                                    "PATTERN" -> "Pattern Lock"
                                    else -> "Protected"
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
                        Text("Change Master Password / PIN")
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
                        text = "Quick Access & Sensors",
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
                                text = "Biometric Quick Unlock",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Unlock with fingerprint or face instead of re-entering master password",
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
                                text = "Emergency Shake-to-Lock",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Instantly lock and exit Hidden Vault when your device is shaken",
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
                    Text(
                        text = "Duress Protection (Decoy Vault)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "If forced to open your vault under duress, enter this separate secondary PIN to display a realistic, empty decoy vault with zero traces of your actual private files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hasDecoyPin) "✓ Decoy PIN Active" else "Decoy PIN Not Set",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (hasDecoyPin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { showDecoyPinDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (hasDecoyPin) "Modify Decoy PIN" else "Set Decoy PIN")
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
                        text = "Stealth Mode",
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
                                text = "Hide Vault from Albums",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Completely hide the Hidden Vault album tile from the Albums screen",
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
                            label = { Text("Search Secret Trigger") },
                            placeholder = { Text("e.g. #vault") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To open vault, type this keyword into the search bar.",
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
                        text = "Encrypted Vault Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Export an encrypted .imava container protected with a custom passphrase to transfer between devices without cloud services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                            Text("Export Backup")
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
                            Text("Import Backup")
                        }
                    }
                }
            }

            // Card 6: Danger Zone / Vault Deletion
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Vault Destruction & Deletion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Permanently remove the vault configuration. You can choose whether to restore private media back to your public gallery or physically shred everything.",
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
                        Text("Delete Vault...")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog: Change Master Password
    if (showPasswordChangeDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newLockType by remember { mutableStateOf(lockType) }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }
        var showNewPass by remember { mutableStateOf(false) }
        var changeError by remember { mutableStateOf<String?>(null) }

        var patternStep by remember { mutableIntStateOf(1) }
        var patternFirst by remember { mutableStateOf("") }
        var patternResult by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPasswordChangeDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Change Master Password") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Verify your current password:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; changeError = null },
                        label = { Text("Current Password / PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Choose new protection type:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newLockType == "PASSWORD", onClick = { newLockType = "PASSWORD"; newPassword = ""; confirmNewPassword = "" })
                        Text("Alphanumeric Password", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newLockType == "PIN", onClick = { newLockType = "PIN"; newPassword = ""; confirmNewPassword = "" })
                        Text("Numeric PIN (4-8 digits)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newLockType == "PATTERN", onClick = { newLockType = "PATTERN"; patternStep = 1; patternFirst = ""; patternResult = "" })
                        Text("Pattern Lock", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (newLockType == "PASSWORD" || newLockType == "PIN") {
                        val isPin = newLockType == "PIN"
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                if (isPin) {
                                    if (it.length <= 8 && it.all { c -> c.isDigit() }) newPassword = it
                                } else newPassword = it
                                changeError = null
                            },
                            label = { Text(if (isPin) "New PIN" else "New Password") },
                            visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showNewPass = !showNewPass }) {
                                    Icon(if (showNewPass) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = {
                                if (isPin) {
                                    if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmNewPassword = it
                                } else confirmNewPassword = it
                                changeError = null
                            },
                            label = { Text("Confirm New Code") },
                            visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(if (patternStep == 1) "Draw new pattern" else "Confirm new pattern", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        PatternLockView(
                            modifier = Modifier.size(220.dp),
                            onPatternComplete = { p ->
                                if (p.split("-").size < 4) changeError = "Min 4 dots"
                                else if (patternStep == 1) {
                                    patternFirst = p
                                    patternStep = 2
                                    changeError = null
                                } else {
                                    if (p == patternFirst) {
                                        patternResult = p
                                        changeError = null
                                    } else {
                                        changeError = "Patterns did not match"
                                        patternStep = 1
                                    }
                                }
                            }
                        )
                    }

                    if (changeError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(changeError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val storedHash = prefs.getString("vault_pin_hash", null)
                        val saltBase64 = prefs.getString("vault_salt", null)
                        val legacyPin = prefs.getString("vault_pin", null)

                        val isOldValid = if (storedHash != null && saltBase64 != null) {
                            val salt = java.util.Base64.getDecoder().decode(saltBase64)
                            VaultCrypto.verifyPin(oldPassword, storedHash, salt).isValid
                        } else if (legacyPin != null) {
                            oldPassword == legacyPin
                        } else true

                        if (!isOldValid) {
                            changeError = "Current password is incorrect"
                            return@Button
                        }

                        val codeToSave = if (newLockType == "PATTERN") patternResult else newPassword.trim()
                        if (newLockType == "PIN" && (codeToSave.length < 4 || codeToSave.length > 8)) {
                            changeError = "PIN must be between 4 and 8 digits"
                            return@Button
                        }
                        if (newLockType == "PASSWORD" && codeToSave.length < 4) {
                            changeError = "Password must be at least 4 characters"
                            return@Button
                        }
                        if ((newLockType == "PIN" || newLockType == "PASSWORD") && newPassword != confirmNewPassword) {
                            changeError = "New passwords do not match"
                            return@Button
                        }
                        if (newLockType == "PATTERN" && patternResult.isBlank()) {
                            changeError = "Please draw and confirm pattern"
                            return@Button
                        }

                        val newSalt = VaultCrypto.generateSalt()
                        val newHash = VaultCrypto.hashPin(codeToSave, newSalt)
                        val encHash = VaultCrypto.encryptString(newHash)
                        val newSaltB64 = java.util.Base64.getEncoder().encodeToString(newSalt)

                        prefs.edit()
                            .putString("vault_lock_type", newLockType)
                            .putString("vault_pin_hash", encHash)
                            .putString("vault_salt", newSaltB64)
                            .remove("vault_pin")
                            .apply()

                        lockType = newLockType
                        showPasswordChangeDialog = false
                        HapticUtil.performSuccess(context)
                        Toast.makeText(context, "Master password updated", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordChangeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Set Decoy PIN
    if (showDecoyPinDialog) {
        var decoyPinInput by remember { mutableStateOf("") }
        var confirmDecoyPinInput by remember { mutableStateOf("") }
        var decoyError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDecoyPinDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Configure Decoy PIN") },
            text = {
                Column {
                    Text(
                        text = "Enter a 4-8 digit numeric PIN different from your real PIN/Password. Entering this code on the unlock screen unlocks an empty decoy vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = decoyPinInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all { c -> c.isDigit() }) decoyPinInput = it
                            decoyError = null
                        },
                        label = { Text("Decoy PIN (4-8 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmDecoyPinInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmDecoyPinInput = it
                            decoyError = null
                        },
                        label = { Text("Confirm Decoy PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (decoyError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(decoyError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (decoyPinInput.length < 4 || decoyPinInput.length > 8) {
                            decoyError = "PIN must be between 4 and 8 digits"
                            return@Button
                        }
                        if (decoyPinInput != confirmDecoyPinInput) {
                            decoyError = "PINs do not match"
                            return@Button
                        }

                        val salt = VaultCrypto.generateSalt()
                        val hash = VaultCrypto.hashPin(decoyPinInput, salt)
                        val encHash = VaultCrypto.encryptString(hash)

                        prefs.edit()
                            .putString("vault_decoy_pin_hash", encHash)
                            .apply()

                        hasDecoyPin = true
                        showDecoyPinDialog = false
                        HapticUtil.performSuccess(context)
                        Toast.makeText(context, "Decoy PIN activated", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Decoy")
                }
            },
            dismissButton = {
                if (hasDecoyPin) {
                    TextButton(
                        onClick = {
                            prefs.edit().remove("vault_decoy_pin_hash").apply()
                            hasDecoyPin = false
                            showDecoyPinDialog = false
                            Toast.makeText(context, "Decoy PIN removed", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Remove Decoy", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = { showDecoyPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete Vault Confirmation
    if (showDeleteConfirmDialog) {
        var restoreMediaOption by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { if (!isProcessingDelete) showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Private Vault?") },
            text = {
                Column {
                    Text("Select what happens to your private files:")
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreMediaOption = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = restoreMediaOption, onClick = { restoreMediaOption = true })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Restore all files to Gallery (Recommended)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Decrypts all photos and videos back to your normal camera/gallery album", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreMediaOption = false }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !restoreMediaOption, onClick = { restoreMediaOption = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cryptographically Shred All Files", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Overwrites all files with zeroes and random data. Data will be completely unrecoverable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingDelete = true
                        viewModel.deleteVault(context, restoreMedia = restoreMediaOption) {
                            isProcessingDelete = false
                            showDeleteConfirmDialog = false
                            HapticUtil.performSuccess(context)
                            Toast.makeText(context, if (restoreMediaOption) "Media restored & Vault removed" else "Vault permanently shredded", Toast.LENGTH_LONG).show()
                            onVaultDeleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isProcessingDelete
                ) {
                    if (isProcessingDelete) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                    } else {
                        Text(if (restoreMediaOption) "Restore & Delete" else "Permanently Shred")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }, enabled = !isProcessingDelete) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Password Dialog
    if (showExportPasswordDialog && pendingExportUri != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isProcessingBackup) {
                    showExportPasswordDialog = false
                    pendingExportUri = null
                    exportPassword = ""
                }
            },
            title = { Text("Backup Passphrase") },
            text = {
                Column {
                    Text("Enter a passphrase to encrypt your .imava backup container:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingExportUri ?: return@Button
                        isProcessingBackup = true
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
                                        exportPassword = ""
                                        pendingExportUri = null
                                    }
                                } else {
                                    Toast.makeText(context, "Could not open file for writing", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessingBackup = false
                            }
                        }
                    },
                    enabled = exportPassword.isNotBlank() && !isProcessingBackup
                ) {
                    if (isProcessingBackup) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportPasswordDialog = false
                    pendingExportUri = null
                    exportPassword = ""
                }, enabled = !isProcessingBackup) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Password Dialog
    if (showRestorePasswordDialog && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isProcessingBackup) {
                    showRestorePasswordDialog = false
                    pendingRestoreUri = null
                    restorePassword = ""
                }
            },
            title = { Text("Enter Backup Passphrase") },
            text = {
                Column {
                    Text("Enter the passphrase used when creating this .imava backup:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingRestoreUri ?: return@Button
                        isProcessingBackup = true
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
                                        restorePassword = ""
                                        pendingRestoreUri = null
                                    }
                                } else {
                                    Toast.makeText(context, "Could not open backup file", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isProcessingBackup = false
                            }
                        }
                    },
                    enabled = restorePassword.isNotBlank() && !isProcessingBackup
                ) {
                    if (isProcessingBackup) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePasswordDialog = false
                    pendingRestoreUri = null
                    restorePassword = ""
                }, enabled = !isProcessingBackup) {
                    Text("Cancel")
                }
            }
        )
    }
}

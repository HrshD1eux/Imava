package com.hrshd1eux.imava.ui.vault

import android.content.Context
import com.hrshd1eux.imava.core.util.findActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.HapticUtil
import com.hrshd1eux.imava.core.util.VaultCrypto
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnlockDialog(
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit,
    onUnlockDecoy: () -> Unit = onUnlockSuccess
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE) }
    val configuration = LocalConfiguration.current
    val activity = remember(context) { context.findActivity() }
    
    val storedLockType = prefs.getString("vault_lock_type", "PIN") ?: "PIN"
    val storedPinHash = prefs.getString("vault_pin_hash", null)
    val storedDecoyHash = prefs.getString("vault_decoy_pin_hash", null)
    val storedSaltBase64 = prefs.getString("vault_salt", null)
    val legacyPin = prefs.getString("vault_pin", null)
    val isVaultDisabled = prefs.getBoolean("vault_disabled", false)
    val isPinConfigured = !isVaultDisabled && (storedPinHash != null || legacyPin != null)

    // Setup Wizard state
    var setupStage by remember { mutableIntStateOf(1) } // 1: Choose Type & Set Code, 2: Additional Security (Biometrics & Stealth)
    var setupLockType by remember { mutableStateOf("PIN") } // "PIN", "PATTERN", or "PASSWORD"
    var setupPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var patternSetupStep by remember { mutableIntStateOf(1) }
    var firstDrawnPattern by remember { mutableStateOf("") }
    var setupBiometrics by remember { mutableStateOf(false) }
    var setupStealthMode by remember { mutableStateOf(false) }
    var setupSecretTrigger by remember { mutableStateOf("#vault") }
    var chosenCodeToSave by remember { mutableStateOf("") }
    var isSetupPassVisible by remember { mutableStateOf(false) }
    var isConfirmPassVisible by remember { mutableStateOf(false) }

    // Unlock state
    var pinInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPatternError by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val initialLockoutUntil = prefs.getLong("vault_lockout_until_ms", 0L)
    val initialLockoutSecs = ((initialLockoutUntil - now) / 1000L).coerceAtLeast(0L).toInt()

    var failedAttempts by remember { mutableIntStateOf(prefs.getInt("vault_failed_attempts", 0)) }
    var lockoutRemainingSeconds by remember { mutableIntStateOf(initialLockoutSecs) }

    val isBiometricEnabled = prefs.getBoolean("vault_biometric_enabled", false)

    // Auto-prompt biometrics once on screen display if enabled
    LaunchedEffect(Unit) {
        if (isPinConfigured && isBiometricEnabled && activity != null && lockoutRemainingSeconds == 0) {
            delay(300L)
            com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
                activity = activity,
                title = context.getString(R.string.unlock_vault_title),
                subtitle = context.getString(R.string.vault_unlock_bio_subtitle),
                onSuccess = { onUnlockSuccess() },
                onError = { _ -> }
            )
        }
    }

    LaunchedEffect(lockoutRemainingSeconds) {
        if (lockoutRemainingSeconds > 0) {
            delay(1000L)
            lockoutRemainingSeconds -= 1
            if (lockoutRemainingSeconds == 0) {
                prefs.edit().remove("vault_lockout_until_ms").apply()
                errorMessage = null
            }
        }
    }

    fun finalizeSetup() {
        val saltBytes = VaultCrypto.generateSalt()
        val saltBase64 = android.util.Base64.encodeToString(saltBytes, android.util.Base64.NO_WRAP)
        val pinHash = VaultCrypto.hashPin(chosenCodeToSave, saltBytes)
        val encryptedHash = VaultCrypto.encryptString(pinHash)

        prefs.edit()
            .putString("vault_pin_hash", encryptedHash)
            .putString("vault_salt", saltBase64)
            .putString("vault_lock_type", setupLockType)
            .putBoolean("vault_biometric_enabled", setupBiometrics)
            .putBoolean("vault_stealth_mode", setupStealthMode)
            .putString("vault_secret_trigger", setupSecretTrigger.trim().ifEmpty { "#vault" })
            .putBoolean("vault_disabled", false)
            .remove("vault_pin")
            .apply()

        HapticUtil.performSuccess(context)
        onUnlockSuccess()
    }

    fun validateAndProceedSetup() {
        val minLen = if (setupLockType == "PIN") 4 else 6
        if (setupPinInput.length < minLen) {
            errorMessage = context.getString(
                if (setupLockType == "PIN") R.string.vault_unlock_err_pin_min_chars_fmt else R.string.vault_unlock_err_pass_min_chars_fmt,
                minLen
            )
            return
        }
        if (setupPinInput != confirmPinInput) {
            errorMessage = context.getString(
                if (setupLockType == "PIN") R.string.vault_unlock_err_pins_mismatch else R.string.vault_unlock_err_passwords_mismatch
            )
            return
        }
        chosenCodeToSave = setupPinInput
        setupStage = 2
        errorMessage = null
    }

    fun verifyInput(input: String) {
        if (lockoutRemainingSeconds > 0) return

        var isValid = false
        var isDecoy = false
        var needsUpgrade = false

        if (storedPinHash != null && storedSaltBase64 != null) {
            val salt = android.util.Base64.decode(storedSaltBase64, android.util.Base64.NO_WRAP)
            val res = VaultCrypto.verifyPin(input, storedPinHash, salt)
            isValid = res.isValid
            needsUpgrade = res.needsUpgrade

            if (!isValid && storedDecoyHash != null) {
                val storedDecoySaltBase64 = prefs.getString("vault_decoy_salt", null)
                val storedDecoyLockType = prefs.getString("vault_decoy_lock_type", null)
                val decoyLockType = storedDecoyLockType ?: storedLockType

                if (decoyLockType == storedLockType) {
                    val decoySalt = if (storedDecoySaltBase64 != null) {
                        try {
                            android.util.Base64.decode(storedDecoySaltBase64, android.util.Base64.NO_WRAP)
                        } catch (_: Exception) {
                            salt
                        }
                    } else {
                        salt
                    }
                    val decoyRes = VaultCrypto.verifyPin(input, storedDecoyHash, decoySalt)
                    if (decoyRes.isValid) {
                        isDecoy = true
                    }
                }
            }
        } else if (legacyPin != null && input == legacyPin) {
            isValid = true
            needsUpgrade = true
        }

        if (isDecoy) {
            failedAttempts = 0
            lockoutRemainingSeconds = 0
            prefs.edit()
                .remove("vault_lockout_until_ms")
                .remove("vault_failed_attempts")
                .apply()
            HapticUtil.performSuccess(context)
            onUnlockDecoy()
            return
        }

        if (isValid) {
            failedAttempts = 0
            lockoutRemainingSeconds = 0
            prefs.edit()
                .remove("vault_lockout_until_ms")
                .remove("vault_failed_attempts")
                .apply()

            if (needsUpgrade) {
                // Seamlessly migrate legacy SHA-256 or plaintext PIN to PBKDF2WithHmacSHA256
                try {
                    val saltBytes = VaultCrypto.generateSalt()
                    val saltBase64 = android.util.Base64.encodeToString(saltBytes, android.util.Base64.NO_WRAP)
                    val newHash = VaultCrypto.hashPin(input, saltBytes)
                    val encHash = VaultCrypto.encryptString(newHash)
                    prefs.edit()
                        .putString("vault_pin_hash", encHash)
                        .putString("vault_salt", saltBase64)
                        .remove("vault_pin")
                        .apply()
                } catch (_: Exception) {}
            }

            HapticUtil.performSuccess(context)
            onUnlockSuccess()
        } else {
            HapticUtil.performError(context)
            val newAttempts = failedAttempts + 1
            if (newAttempts >= 3) {
                val lockoutExpiry = System.currentTimeMillis() + 30_000L
                prefs.edit()
                    .putLong("vault_lockout_until_ms", lockoutExpiry)
                    .putInt("vault_failed_attempts", 0)
                    .apply()
                lockoutRemainingSeconds = 30
                failedAttempts = 0
                errorMessage = context.getString(R.string.vault_unlock_err_lockout_30s)
            } else {
                failedAttempts = newAttempts
                prefs.edit().putInt("vault_failed_attempts", newAttempts).apply()
                errorMessage = context.getString(R.string.vault_unlock_err_attempt_fmt, newAttempts)
            }
            isPatternError = true
            pinInput = ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (!isPinConfigured) {
                                    if (setupStage == 1) stringResource(R.string.vault_unlock_setup_step1) else stringResource(R.string.vault_unlock_setup_step2)
                                } else {
                                    stringResource(R.string.unlock_vault_title)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_cancel)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                com.hrshd1eux.imava.ui.common.AutoFitText(
                                    text = stringResource(R.string.action_cancel),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    minFontSize = 10.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isPinConfigured && isBiometricEnabled && activity != null) {
                                    TextButton(
                                        onClick = {
                                            com.hrshd1eux.imava.core.util.BiometricAuthHelper.authenticate(
                                                activity = activity,
                                                title = context.getString(R.string.unlock_vault_title),
                                                subtitle = context.getString(R.string.vault_unlock_bio_subtitle),
                                                onSuccess = { onUnlockSuccess() },
                                                onError = { _ -> }
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        com.hrshd1eux.imava.ui.common.AutoFitText(
                                            text = stringResource(R.string.vault_unlock_use_bio_btn),
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            minFontSize = 9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (!isPinConfigured) {
                                    if (setupStage == 2) {
                                        TextButton(onClick = { setupStage = 1 }) {
                                            com.hrshd1eux.imava.ui.common.AutoFitText(
                                                text = stringResource(R.string.action_back),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                minFontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { finalizeSetup() }) {
                                            com.hrshd1eux.imava.ui.common.AutoFitText(
                                                text = stringResource(R.string.vault_unlock_finish_btn),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                minFontSize = 10.sp
                                            )
                                        }
                                    } else if (setupLockType != "PATTERN") {
                                        Button(onClick = { validateAndProceedSetup() }) {
                                            com.hrshd1eux.imava.ui.common.AutoFitText(
                                                text = stringResource(R.string.action_next),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                minFontSize = 10.sp
                                            )
                                        }
                                    }
                                } else {
                                    if (storedLockType != "PATTERN") {
                                        Button(
                                            onClick = {
                                                if (pinInput.isNotBlank()) {
                                                    verifyInput(pinInput)
                                                }
                                            },
                                            enabled = pinInput.isNotBlank() && lockoutRemainingSeconds == 0
                                        ) {
                                            com.hrshd1eux.imava.ui.common.AutoFitText(
                                                text = stringResource(R.string.action_unlock),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                minFontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isPinConfigured) {
                            // --- REFINED UNLOCK VIEW ---
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.unlock_vault_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (storedLockType) {
                                    "PASSWORD" -> stringResource(R.string.vault_set_type_aes256)
                                    "PIN" -> stringResource(R.string.vault_dlg_numeric_pin)
                                    "PATTERN" -> stringResource(R.string.vault_set_type_pattern)
                                    else -> stringResource(R.string.vault_set_type_protected)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            if (lockoutRemainingSeconds > 0) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.vault_unlock_lockout_fmt, lockoutRemainingSeconds),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else if (errorMessage != null) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (storedLockType == "PATTERN") {
                                Text(
                                    text = if (lockoutRemainingSeconds > 0) stringResource(R.string.vault_unlock_temp_locked) else stringResource(R.string.vault_unlock_draw_unlock),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                if (lockoutRemainingSeconds == 0) {
                                    val patternSize = minOf(configuration.screenWidthDp.dp * 0.76f, configuration.screenHeightDp.dp * 0.38f, 280.dp)
                                    Surface(
                                        tonalElevation = 2.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        PatternLockView(
                                            onPatternComplete = { patternStr ->
                                                isPatternError = false
                                                verifyInput(patternStr)
                                            },
                                            isError = isPatternError,
                                            modifier = Modifier.size(patternSize).padding(12.dp)
                                        )
                                    }
                                }
                            } else if (storedLockType == "PASSWORD") {
                                OutlinedTextField(
                                    value = pinInput,
                                    enabled = lockoutRemainingSeconds == 0,
                                    onValueChange = {
                                        pinInput = it
                                        errorMessage = null
                                    },
                                    label = { Text(stringResource(R.string.vault_unlock_enter_master_pass)) },
                                    singleLine = true,
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (pinInput.isNotBlank()) {
                                                verifyInput(pinInput)
                                            }
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                )

                                // Inline primary Unlock button ensures users on all screen sizes can always submit
                                Button(
                                    onClick = {
                                        if (pinInput.isNotBlank()) {
                                            verifyInput(pinInput)
                                        }
                                    },
                                    enabled = pinInput.isNotBlank() && lockoutRemainingSeconds == 0,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.action_unlock),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // PIN Lock type
                                OutlinedTextField(
                                    value = pinInput,
                                    enabled = lockoutRemainingSeconds == 0,
                                    onValueChange = {
                                        if (it.all { c -> c.isDigit() } && it.length <= 16) {
                                            pinInput = it
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text(stringResource(R.string.vault_enter_pin_label)) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (pinInput.isNotBlank()) {
                                                verifyInput(pinInput)
                                            }
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                )

                                // Inline primary Unlock button
                                Button(
                                    onClick = {
                                        if (pinInput.isNotBlank()) {
                                            verifyInput(pinInput)
                                        }
                                    },
                                    enabled = pinInput.isNotBlank() && lockoutRemainingSeconds == 0,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.action_unlock),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // --- SETUP WIZARD ---
                            if (setupStage == 1) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = stringResource(R.string.vault_unlock_zero_knowledge_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.vault_unlock_zero_knowledge_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (errorMessage != null) {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    ) {
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = stringResource(R.string.vault_unlock_choose_protect),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { setupLockType = "PIN"; errorMessage = null },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (setupLockType == "PIN") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.vault_type_pin_chip), style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = { setupLockType = "PATTERN"; errorMessage = null },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (setupLockType == "PATTERN") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.vault_unlock_type_pattern), style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = { setupLockType = "PASSWORD"; errorMessage = null },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (setupLockType == "PASSWORD") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.vault_unlock_type_password), style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                if (setupLockType == "PIN") {
                                    OutlinedTextField(
                                        value = setupPinInput,
                                        onValueChange = {
                                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                                setupPinInput = it
                                                errorMessage = null
                                            }
                                        },
                                        label = { Text(stringResource(R.string.vault_unlock_pin_4_6_label)) },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                                    )
                                    OutlinedTextField(
                                        value = confirmPinInput,
                                        onValueChange = {
                                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                                confirmPinInput = it
                                                errorMessage = null
                                            }
                                        },
                                        label = { Text(stringResource(R.string.vault_confirm_pin_label)) },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { validateAndProceedSetup() }),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )
                                    Button(
                                        onClick = { validateAndProceedSetup() },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text(stringResource(R.string.action_next), fontWeight = FontWeight.Bold)
                                    }
                                } else if (setupLockType == "PASSWORD") {
                                    OutlinedTextField(
                                        value = setupPinInput,
                                        onValueChange = {
                                            setupPinInput = it
                                            errorMessage = null
                                        },
                                        label = { Text(stringResource(R.string.vault_unlock_master_pass_label)) },
                                        singleLine = true,
                                        visualTransformation = if (isSetupPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isSetupPassVisible = !isSetupPassVisible }) {
                                                Icon(
                                                    imageVector = if (isSetupPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                                    )
                                    OutlinedTextField(
                                        value = confirmPinInput,
                                        onValueChange = {
                                            confirmPinInput = it
                                            errorMessage = null
                                        },
                                        label = { Text(stringResource(R.string.vault_unlock_confirm_master_pass_label)) },
                                        singleLine = true,
                                        visualTransformation = if (isConfirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isConfirmPassVisible = !isConfirmPassVisible }) {
                                                Icon(
                                                    imageVector = if (isConfirmPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { validateAndProceedSetup() }),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                    )
                                    Button(
                                        onClick = { validateAndProceedSetup() },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text(stringResource(R.string.action_next), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // Pattern setup
                                    Text(
                                        text = if (patternSetupStep == 1) stringResource(R.string.vault_draw_pattern_min4) else stringResource(R.string.vault_unlock_draw_pattern_again),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    val patternSize = minOf(configuration.screenWidthDp.dp * 0.76f, configuration.screenHeightDp.dp * 0.38f, 280.dp)
                                    Surface(
                                        tonalElevation = 2.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        PatternLockView(
                                            onPatternComplete = { drawn ->
                                                if (patternSetupStep == 1) {
                                                    if (drawn.split("-").size < 4) {
                                                        errorMessage = context.getString(R.string.vault_connect_min4_dots)
                                                        isPatternError = true
                                                    } else {
                                                        firstDrawnPattern = drawn
                                                        patternSetupStep = 2
                                                        errorMessage = null
                                                        isPatternError = false
                                                    }
                                                } else {
                                                    if (drawn == firstDrawnPattern) {
                                                        chosenCodeToSave = drawn
                                                        setupStage = 2
                                                        errorMessage = null
                                                        isPatternError = false
                                                    } else {
                                                        errorMessage = context.getString(R.string.vault_err_patterns_mismatch_retry)
                                                        patternSetupStep = 1
                                                        isPatternError = true
                                                    }
                                                }
                                            },
                                            isError = isPatternError,
                                            modifier = Modifier.size(patternSize).padding(12.dp)
                                        )
                                    }
                                }
                            } else {
                                // Stage 2: Biometrics & Stealth Mode
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = stringResource(R.string.vault_unlock_config_optional),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(stringResource(R.string.vault_unlock_use_bio_btn), style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    stringResource(R.string.vault_unlock_bio_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = setupBiometrics,
                                                onCheckedChange = { setupBiometrics = it }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(stringResource(R.string.vault_stealth_mode_title), style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    stringResource(R.string.vault_unlock_stealth_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = setupStealthMode,
                                                onCheckedChange = { setupStealthMode = it }
                                            )
                                        }

                                        if (setupStealthMode) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedTextField(
                                                value = setupSecretTrigger,
                                                onValueChange = { setupSecretTrigger = it },
                                                label = { Text(stringResource(R.string.vault_dlg_secret_search_phrase)) },
                                                placeholder = { Text("#vault") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { finalizeSetup() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.vault_unlock_finish_btn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

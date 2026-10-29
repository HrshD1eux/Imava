package com.hrshd1eux.imava.ui.vault

import android.content.Context
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.hrshd1eux.imava.core.util.VaultCrypto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSetupScreen(
    onBackClick: () -> Unit,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE) }

    // Lock type: "PIN", "PASSWORD", or "PATTERN"
    var lockType by remember { mutableStateOf("PASSWORD") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var patternSetupStep by remember { mutableIntStateOf(1) }
    var firstPattern by remember { mutableStateOf("") }
    var patternInput by remember { mutableStateOf("") }

    var enableBiometrics by remember { mutableStateOf(false) }
    var enableStealthMode by remember { mutableStateOf(false) }
    var secretTrigger by remember { mutableStateOf("#vault") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Set Up Private Vault",
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
            // Prominent Zero-Knowledge Local-First Warning Banner
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Zero-Knowledge Encryption Warning",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Private Vault is completely local-first with zero tracking. There is NO password reset, NO backdoor, and NO cloud recovery. If you forget this password, all files inside the vault are permanently lost.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Lock Type Selection Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose Protection Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 1: Mixed Alphanumeric Master Password
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lockType = "PASSWORD"
                                passwordInput = ""
                                confirmPasswordInput = ""
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = lockType == "PASSWORD",
                            onClick = {
                                lockType = "PASSWORD"
                                passwordInput = ""
                                confirmPasswordInput = ""
                                errorMessage = null
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Alphanumeric Master Password (Recommended)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Set a master password to encrypt your private files with AES-256 (supports letters, numbers, and symbols).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Option 2: 4-8 Digit Numeric PIN
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lockType = "PIN"
                                passwordInput = ""
                                confirmPasswordInput = ""
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = lockType == "PIN",
                            onClick = {
                                lockType = "PIN"
                                passwordInput = ""
                                confirmPasswordInput = ""
                                errorMessage = null
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Numeric PIN",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "4 to 8 digit quick unlock PIN.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Option 3: Pattern Lock
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lockType = "PATTERN"
                                patternSetupStep = 1
                                firstPattern = ""
                                patternInput = ""
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = lockType == "PATTERN",
                            onClick = {
                                lockType = "PATTERN"
                                patternSetupStep = 1
                                firstPattern = ""
                                patternInput = ""
                                errorMessage = null
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Pattern Lock",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Connect at least 4 dots on a 3x3 grid.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Input Fields Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (lockType == "PASSWORD" || lockType == "PIN") {
                        val isPin = lockType == "PIN"
                        Text(
                            text = if (isPin) "Set Numeric PIN" else "Set Master Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                if (isPin) {
                                    if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                        passwordInput = it
                                        errorMessage = null
                                    }
                                } else {
                                    passwordInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text(if (isPin) "Enter PIN (4-8 digits)" else "Master Password (min 4 chars)") },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (showPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = {
                                if (isPin) {
                                    if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                        confirmPasswordInput = it
                                        errorMessage = null
                                    }
                                } else {
                                    confirmPasswordInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text(if (isPin) "Confirm PIN" else "Confirm Master Password") },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Pattern mode
                        Text(
                            text = if (patternSetupStep == 1) "Draw your unlock pattern (min 4 dots)" else "Draw pattern again to confirm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PatternLockView(
                                modifier = Modifier.size(260.dp),
                                onPatternComplete = { patternStr ->
                                    if (patternStr.split("-").size < 4) {
                                        errorMessage = "Connect at least 4 dots"
                                    } else if (patternSetupStep == 1) {
                                        firstPattern = patternStr
                                        patternSetupStep = 2
                                        errorMessage = null
                                        HapticUtil.performSelection(context)
                                    } else {
                                        if (patternStr == firstPattern) {
                                            patternInput = patternStr
                                            errorMessage = null
                                            HapticUtil.performSuccess(context)
                                        } else {
                                            errorMessage = "Patterns did not match. Try again."
                                            patternSetupStep = 1
                                            firstPattern = ""
                                        }
                                    }
                                }
                            )

                            if (patternSetupStep == 2 && patternInput.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✓ Pattern confirmed",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Quick Access & Biometrics Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Access & Stealth Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                                text = "Use fingerprint or face recognition for fast unlock without entering password every time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableBiometrics,
                            onCheckedChange = { enableBiometrics = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stealth Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Completely hide Hidden Vault from Albums screen. Access only by typing secret code in search.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableStealthMode,
                            onCheckedChange = { enableStealthMode = it }
                        )
                    }

                    if (enableStealthMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = secretTrigger,
                            onValueChange = { secretTrigger = it },
                            label = { Text("Search Secret Trigger") },
                            placeholder = { Text("e.g. #vault") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save & Complete Button
            Button(
                onClick = {
                    val codeToSave = if (lockType == "PATTERN") patternInput else passwordInput.trim()

                    if (lockType == "PIN" && (codeToSave.length < 4 || codeToSave.length > 8)) {
                        errorMessage = "PIN must be between 4 and 8 digits"
                        return@Button
                    }
                    if (lockType == "PASSWORD" && codeToSave.length < 4) {
                        errorMessage = "Password must be at least 4 characters"
                        return@Button
                    }
                    if ((lockType == "PIN" || lockType == "PASSWORD") && passwordInput != confirmPasswordInput) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }
                    if (lockType == "PATTERN" && patternInput.isBlank()) {
                        errorMessage = "Please draw and confirm a pattern"
                        return@Button
                    }

                    val salt = VaultCrypto.generateSalt()
                    val hash = VaultCrypto.hashPin(codeToSave, salt)
                    val encryptedHash = VaultCrypto.encryptString(hash)
                    val saltBase64 = java.util.Base64.getEncoder().encodeToString(salt)

                    prefs.edit()
                        .putString("vault_lock_type", lockType)
                        .putString("vault_pin_hash", encryptedHash)
                        .putString("vault_salt", saltBase64)
                        .putBoolean("vault_biometric_enabled", enableBiometrics)
                        .putBoolean("vault_stealth_mode", enableStealthMode)
                        .putString("vault_secret_trigger", secretTrigger)
                        .putBoolean("vault_disabled", false)
                        .remove("vault_failed_attempts")
                        .remove("vault_lockout_until_ms")
                        .remove("vault_pin") // purge legacy
                        .apply()

                    HapticUtil.performSuccess(context)
                    android.widget.Toast.makeText(context, "Private Vault secured with AES-256", android.widget.Toast.LENGTH_SHORT).show()
                    onSetupComplete()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create & Secure Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

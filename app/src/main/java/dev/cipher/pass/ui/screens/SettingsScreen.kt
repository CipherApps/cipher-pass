package dev.cipher.pass.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.pass.crypto.BiometricPromptManager
import dev.cipher.pass.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showRemovePinConfirm by remember { mutableStateOf(false) }
    var newPinValue by remember { mutableStateOf("") }
    var showBackupTypeDialog by remember { mutableStateOf(false) }
    var showCsvWarningDialog by remember { mutableStateOf(false) }
    var showCipherExportPasswordDialog by remember { mutableStateOf(false) }
    var showCipherImportPasswordDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingExportCipherUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val useDynamicColors by viewModel.useDynamicColors.collectAsState(initial = true)
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState(initial = false)
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState(initial = true)
    val currentPin by viewModel.appPin.collectAsState(initial = null)
    val backupState by viewModel.backupState.collectAsState()

    val context = LocalContext.current
    val isHardwareBiometricAvailable = remember {
        BiometricPromptManager.canAuthenticate(context)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(backupState) {
        backupState?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearState()
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportToCsv(context, it) }
    }

    val exportCipherLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            pendingExportCipherUri = it
            showCipherExportPasswordDialog = true
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            val contentResolver = context.contentResolver
            val fileName = runCatching {
                contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
                }
            }.getOrNull() ?: ""

            if (fileName.endsWith(".csv", ignoreCase = true)) {
                viewModel.importFromCsv(context, selectedUri)
            } else {
                pendingImportUri = selectedUri
                showCipherImportPasswordDialog = true
            }
        }
    }

    if (showBackupTypeDialog) {
        AlertDialog(
            onDismissRequest = { showBackupTypeDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Export Vault Backup", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Choose the export format for your password database:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )

                    Surface(
                        onClick = {
                            showBackupTypeDialog = false
                            exportCipherLauncher.launch("cipher_backup.cipher")
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.EnhancedEncryption, contentDescription = null, tint = primaryColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Encrypted (.cipher)", fontWeight = FontWeight.Bold)
                                Text("Password protected. Safe to store anywhere.", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            showBackupTypeDialog = false
                            showCsvWarningDialog = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Plaintext CSV (.csv)", fontWeight = FontWeight.Bold)
                                Text("Unencrypted text. Easy to import in other apps.", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBackupTypeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCsvWarningDialog) {
        AlertDialog(
            onDismissRequest = { showCsvWarningDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Security Warning!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    "CSV files are NOT encrypted. Anyone with access to the file will be able to read all your passwords in plain text. Are you sure?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCsvWarningDialog = false
                        exportCsvLauncher.launch("passwords_backup.csv")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Export Unencrypted CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvWarningDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCipherExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showCipherExportPasswordDialog = false
                backupPassword = ""
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Set Backup Password", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a password to encrypt this backup file. You will need this password to restore your vault later.", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant)
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingExportCipherUri?.let { uri ->
                            viewModel.exportToCipher(context, uri, backupPassword)
                        }
                        showCipherExportPasswordDialog = false
                        backupPassword = ""
                        pendingExportCipherUri = null
                    },
                    enabled = backupPassword.length >= 4,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Encrypt & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCipherExportPasswordDialog = false
                    backupPassword = ""
                    pendingExportCipherUri = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showCipherImportPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showCipherImportPasswordDialog = false
                backupPassword = ""
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Enter Backup Password", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This file is encrypted. Enter the password used when creating this backup.", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant)
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingImportUri?.let { uri ->
                            viewModel.importFromCipher(context, uri, backupPassword)
                        }
                        showCipherImportPasswordDialog = false
                        backupPassword = ""
                        pendingImportUri = null
                    },
                    enabled = backupPassword.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Decrypt & Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCipherImportPasswordDialog = false
                    backupPassword = ""
                    pendingImportUri = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                newPinValue = ""
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Set Master PIN / Password", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter a password or PIN code to encrypt and unlock your vault on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPinValue,
                        onValueChange = { newPinValue = it },
                        label = { Text("New Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinValue.isNotEmpty()) {
                            viewModel.setAppPin(newPinValue)
                            viewModel.setAppLock(true)
                            showPinDialog = false
                            newPinValue = ""
                        }
                    },
                    enabled = newPinValue.length >= 4,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    newPinValue = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRemovePinConfirm) {
        AlertDialog(
            onDismissRequest = { showRemovePinConfirm = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Remove Lock Code?") },
            text = { Text("This will disable App Lock and clear your master unlock code. Your vault items will no longer be protected locally.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAppPin(null)
                        viewModel.setAppLock(false)
                        showRemovePinConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text("Nuclear Option", style = MaterialTheme.typography.headlineSmall, color = onSurface)
            },
            text = {
                Text(
                    "This will permanently delete ALL saved passwords, logins, and encrypted vault entries. This action CANNOT be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.nuclearWipe()
                        showDeleteDialog = false
                        onBack()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Wipe Vault", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = primaryColor)
                }
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Open Source Licenses", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    val libs = listOf(
                        "Jetpack Compose" to "Apache License 2.0",
                        "Navigation Compose" to "Apache License 2.0",
                        "Dagger Hilt" to "Apache License 2.0",
                        "Room Database" to "Apache License 2.0",
                        "AndroidX Security-Crypto" to "Apache License 2.0",
                        "Jetpack DataStore" to "Apache License 2.0",
                        "AndroidX Core SplashScreen" to "Apache License 2.0",
                        "Kotlin Coroutines & Flow" to "Apache License 2.0",
                        "AndroidX Biometric" to "Apache License 2.0"
                    )

                    libs.forEach { (name, license) ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(name, style = MaterialTheme.typography.labelLarge, color = primaryColor)
                            Text(license, style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Close", color = primaryColor)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(title = "APPEARANCE") {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Palette,
                    title = "Dynamic Colors",
                    subtitle = "Match app colors to your wallpaper (Android 12+)",
                    checked = useDynamicColors,
                    onCheckedChange = { viewModel.setDynamicColors(it) }
                )
            }

            SettingsSection(title = "PRIVACY & VAULT SECURITY") {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Lock,
                    title = "App Lock",
                    subtitle = "Require authentication to open vault",
                    checked = isAppLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && currentPin == null) {
                            showPinDialog = true
                        } else {
                            viewModel.setAppLock(enabled)
                        }
                    }
                )

                if (isAppLockEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SettingsSwitchRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = "Biometric Unlock",
                        subtitle = if (isHardwareBiometricAvailable) "Use fingerprint or face recognition"
                        else "Biometric authentication not available on this device",
                        checked = isBiometricEnabled && isHardwareBiometricAvailable,
                        enabled = isHardwareBiometricAvailable,
                        onCheckedChange = { viewModel.setBiometric(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SettingsClickableRow(
                        icon = Icons.Outlined.Password,
                        title = "Change Master Passcode",
                        subtitle = if (currentPin == null) "Passcode not set" else "Update your security passcode or PIN",
                        onClick = { showPinDialog = true }
                    )

                    if (currentPin != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsClickableRow(
                            icon = Icons.Outlined.LockOpen,
                            title = "Remove Passcode",
                            subtitle = "Disables lock and clears security code",
                            isWarning = true,
                            onClick = { showRemovePinConfirm = true }
                        )
                    }
                }
            }

            SettingsSection(title = "BACKUP & RESTORE") {
                SettingsClickableRow(
                    icon = Icons.Outlined.FileUpload,
                    title = "Export Vault Backup",
                    subtitle = "Save encrypted .cipher file or unencrypted CSV",
                    onClick = { showBackupTypeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingsClickableRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "Import Vault Backup",
                    subtitle = "Restore entries from .cipher or CSV file",
                    onClick = { importLauncher.launch("*/*") }
                )
            }

            SettingsSection(title = "DANGER ZONE") {
                SettingsClickableRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Nuclear Wipe",
                    subtitle = "Permanently destroy all passwords and data",
                    isDanger = true,
                    onClick = { showDeleteDialog = true }
                )
            }

            SettingsSection(title = "ABOUT") {
                SettingsClickableRow(
                    icon = Icons.Outlined.Shield,
                    title = "Encryption",
                    subtitle = "On-device AES-256 GCM encryption",
                    onClick = { }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingsClickableRow(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = "Open Source Licenses",
                    subtitle = "Legal information and tech stack",
                    onClick = { showLicensesDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingsClickableRow(
                    icon = Icons.Outlined.Language,
                    title = "Project Website",
                    subtitle = "cipherapps.github.io",
                    onClick = { uriHandler.openUri("https://cipherapps.github.io/") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CipherPass",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version 1.0.1",
                    fontSize = 12.sp,
                    color = onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "© 2026 CipherApps",
                    fontSize = 10.sp,
                    color = onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(icon = icon, enabled = enabled)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDanger: Boolean = false,
    isWarning: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(
            icon = icon,
            isDanger = isDanger,
            isWarning = isWarning
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isDanger || isWarning -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = when {
                    isDanger || isWarning -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SettingsIconBox(
    icon: ImageVector,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    isWarning: Boolean = false
) {
    val bgColor = when {
        isDanger || isWarning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val iconColor = when {
        isDanger || isWarning -> MaterialTheme.colorScheme.error
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.size(36.dp),
        color = bgColor,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
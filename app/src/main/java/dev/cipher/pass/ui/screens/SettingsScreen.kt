package dev.cipher.pass.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
    var showCipherExportDialog by remember { mutableStateOf(false) }
    var showCipherImportDialog by remember { mutableStateOf(false) }
    var newPinValue by remember { mutableStateOf("") }
    var cipherPassword by remember { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val useDynamicColors by viewModel.useDynamicColors.collectAsState(initial = true)
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState(initial = false)

    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState(initial = true)
    val currentPin by viewModel.appPin.collectAsState(initial = null)
    val backupState by viewModel.backupState.collectAsState()

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isHardwareBiometricAvailable = remember {
        BiometricPromptManager.canAuthenticate(context)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? -> uri?.let { viewModel.exportToCsv(context, it) } }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.importFromCsv(context, it) } }

    val exportCipherLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            showCipherExportDialog = true
        }
    }
    val importCipherLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            showCipherImportDialog = true
        }
    }

    LaunchedEffect(backupState) {
        backupState?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearState()
        }
    }


    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; newPinValue = "" },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Set App PIN", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a 4-digit PIN to secure your passwords.", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant)
                    OutlinedTextField(
                        value = newPinValue,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinValue = it },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinValue.length == 4) {
                            viewModel.setAppPin(newPinValue)
                            viewModel.setAppLock(true)
                            showPinDialog = false
                            newPinValue = ""
                        }
                    },
                    enabled = newPinValue.length == 4,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save & Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; newPinValue = "" }) { Text("Cancel") }
            }
        )
    }


    if (showRemovePinConfirm) {
        AlertDialog(
            onDismissRequest = { showRemovePinConfirm = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Remove PIN?") },
            text = { Text("This will disable App Lock and delete your security code. Your passwords will no longer be protected by this PIN.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAppPin(null)
                        viewModel.setAppLock(false)
                        showRemovePinConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePinConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showCipherExportDialog) {
        AlertDialog(
            onDismissRequest = { showCipherExportDialog = false; cipherPassword = "" },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Encrypt Backup", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a password to encrypt your backup file.", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant)
                    OutlinedTextField(
                        value = cipherPassword,
                        onValueChange = { cipherPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingUri?.let { viewModel.exportToCipher(context, it, cipherPassword) }
                        showCipherExportDialog = false
                        cipherPassword = ""
                    },
                    enabled = cipherPassword.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = { showCipherExportDialog = false; cipherPassword = "" }) { Text("Cancel") }
            }
        )
    }

    if (showCipherImportDialog) {
        AlertDialog(
            onDismissRequest = { showCipherImportDialog = false; cipherPassword = "" },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Decrypt Backup", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the password used to encrypt this backup.", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant)
                    OutlinedTextField(
                        value = cipherPassword,
                        onValueChange = { cipherPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingUri?.let { viewModel.importFromCipher(context, it, cipherPassword) }
                        showCipherImportDialog = false
                        cipherPassword = ""
                    },
                    enabled = cipherPassword.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showCipherImportDialog = false; cipherPassword = "" }) { Text("Cancel") }
            }
        )
    }


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Nuclear Option", style = MaterialTheme.typography.headlineSmall, color = onSurface) },
            text = { Text("This will permanently delete ALL vault entries and settings. Action cannot be undone.", style = MaterialTheme.typography.bodyMedium, color = onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { viewModel.nuclearWipe(); showDeleteDialog = false; onBack() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = primaryColor) }
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
                TextButton(onClick = { showLicensesDialog = false }) { Text("Close", color = primaryColor) }
            }
        )
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Appearance", style = MaterialTheme.typography.labelLarge, color = primaryColor, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Dynamic Colors", color = onSurface) },
                    supportingContent = { Text("Match app colors to wallpaper (Android 12+)", color = onSurfaceVariant) },
                    leadingContent = { Icon(Icons.Rounded.Palette, null, tint = primaryColor) },
                    trailingContent = {
                        Switch(checked = useDynamicColors, onCheckedChange = { viewModel.setDynamicColors(it) })
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))


            Text("Backup & Data", style = MaterialTheme.typography.labelLarge, color = primaryColor, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        modifier = Modifier.clickable { exportCipherLauncher.launch("vault_backup.cipher") },
                        headlineContent = { Text("Export Encrypted Backup", color = onSurface) },
                        supportingContent = { Text("Password-protected .cipher container", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.Lock, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = onSurfaceVariant.copy(alpha = 0.1f))
                    ListItem(
                        modifier = Modifier.clickable { importCipherLauncher.launch("*/*") },
                        headlineContent = { Text("Import Encrypted Backup", color = onSurface) },
                        supportingContent = { Text("Restore entries from a .cipher file", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.Key, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = onSurfaceVariant.copy(alpha = 0.1f))
                    ListItem(
                        modifier = Modifier.clickable { exportCsvLauncher.launch("cipherpass_backup.csv") },
                        headlineContent = { Text("Export Unencrypted CSV", color = onSurface) },
                        supportingContent = { Text("Plaintext backup for external tools", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.FileDownload, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = onSurfaceVariant.copy(alpha = 0.1f))
                    ListItem(
                        modifier = Modifier.clickable { importCsvLauncher.launch("text/*") },
                        headlineContent = { Text("Import from CSV", color = onSurface) },
                        supportingContent = { Text("Restore entries from a CSV file", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.FileUpload, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            Text("Privacy & Safety", style = MaterialTheme.typography.labelLarge, color = primaryColor, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("App Lock", color = onSurface) },
                        supportingContent = { Text("Require authentication to open app", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.Lock, null, tint = primaryColor) },
                        trailingContent = {
                            Switch(
                                checked = isAppLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && currentPin == null) showPinDialog = true
                                    else viewModel.setAppLock(enabled)
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    if (isAppLockEnabled) {
                        ListItem(
                            headlineContent = { Text("Biometric Unlock", color = onSurface) },
                            supportingContent = { Text(if (isHardwareBiometricAvailable) "Use fingerprint or face recognition" else "Biometric authentication not available on this device", color = onSurfaceVariant) },
                            leadingContent = { Icon(Icons.Rounded.Fingerprint, null, tint = primaryColor) },
                            trailingContent = {
                                Switch(
                                    checked = isBiometricEnabled && isHardwareBiometricAvailable,
                                    enabled = isHardwareBiometricAvailable,
                                    onCheckedChange = { viewModel.setBiometric(it) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            modifier = Modifier.clickable { showPinDialog = true },
                            headlineContent = { Text("Change App PIN", color = onSurface) },
                            supportingContent = { Text(if (currentPin == null) "PIN not set" else "Update your 4-digit security code", color = onSurfaceVariant) },
                            leadingContent = { Icon(Icons.Rounded.Password, null, tint = primaryColor) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (currentPin != null) {
                            ListItem(
                                modifier = Modifier.clickable { showRemovePinConfirm = true },
                                headlineContent = { Text("Remove App PIN", color = onSurface) },
                                supportingContent = { Text("Disables lock and clears security code", color = onSurfaceVariant) },
                                leadingContent = { Icon(Icons.Rounded.LockOpen, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = onSurfaceVariant.copy(alpha = 0.1f))

                    ListItem(
                        modifier = Modifier.clickable { showDeleteDialog = true },
                        headlineContent = { Text("Nuclear Wipe", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Permanently destroy all vault items", color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) },
                        leadingContent = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            Text("About", style = MaterialTheme.typography.labelLarge, color = primaryColor, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Encryption", color = onSurface) },
                        supportingContent = { Text("On-device AES-256 GCM encryption", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Rounded.Shield, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = onSurfaceVariant.copy(alpha = 0.1f))

                    ListItem(
                        modifier = Modifier.clickable { showLicensesDialog = true },
                        headlineContent = { Text("Open Source Licenses", color = onSurface) },
                        supportingContent = { Text("Legal information and tech stack", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = onSurfaceVariant.copy(alpha = 0.1f))

                    ListItem(
                        modifier = Modifier.clickable { uriHandler.openUri("https://cipherapps.github.io/") },
                        headlineContent = { Text("Project Website", color = onSurface) },
                        supportingContent = { Text("cipherapps.github.io", color = onSurfaceVariant) },
                        leadingContent = { Icon(Icons.Default.Language, null, tint = primaryColor) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))


            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CipherPass", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onSurface)
                Text("Version 1.1.0", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
                Text("© 2026 CipherApps", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}
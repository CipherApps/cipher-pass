package dev.cipher.pass.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.cipher.pass.crypto.BiometricPromptManager
import dev.cipher.pass.ui.viewmodel.SettingsViewModel

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var inputPassword by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val savedPin by viewModel.appPin.collectAsState(initial = null)
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState(initial = true)

    val context = LocalContext.current
    val canUseBiometric = remember { BiometricPromptManager.canAuthenticate(context) }

    fun triggerBiometrics() {
        if (isBiometricEnabled && canUseBiometric) {
            BiometricPromptManager.showPrompt(
                context = context,
                title = "CipherPass",
                subtitle = "Authenticate to unlock your vault",
                onSuccess = { onUnlock() },
                onError = { }
            )
        }
    }

    fun verifyAndUnlock() {
        if (savedPin == null || inputPassword == savedPin) {
            isError = false
            onUnlock()
        } else {
            isError = true
            inputPassword = ""
        }
    }

    LaunchedEffect(isBiometricEnabled, canUseBiometric) {
        if (isBiometricEnabled && canUseBiometric) {
            triggerBiometrics()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = "Shield Icon",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CipherPass is Locked",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter passcode or use biometrics\nto access your vault",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        val maxDots = (savedPin?.length ?: 4).coerceIn(4, 8)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            repeat(maxDots) { index ->
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = when {
                        isError -> MaterialTheme.colorScheme.error
                        index < inputPassword.length -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ) {}
            }
        }

        AnimatedVisibility(
            visible = isError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "Incorrect passcode",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        OutlinedTextField(
            value = inputPassword,
            onValueChange = {
                if (it.length <= 16) {
                    inputPassword = it
                    if (isError) isError = false
                    if (savedPin != null && it.length == savedPin?.length) {
                        verifyAndUnlock()
                    }
                }
            },
            placeholder = {
                Text(
                    "Passcode",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { verifyAndUnlock() }),
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isBiometricEnabled && canUseBiometric) {
            Button(
                onClick = { triggerBiometrics() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock with Biometrics",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
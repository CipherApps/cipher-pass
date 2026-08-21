package dev.cipher.pass.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentGreen,
    tertiary = AccentGreen,
    background = VoidBg,
    surface = CardBg,
    surfaceVariant = SurfaceDark,
    error = DangerRed,
    errorContainer = DangerRedLight,
    onPrimary = VoidBg,
    onSecondary = VoidBg,
    onTertiary = VoidBg,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = TextDim,
    onError = Color.White,
    outline = BorderColor,
    outlineVariant = BorderLight
)

@Composable
fun CipherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CipherTypography,
        content = content
    )
}
package dev.cipher.pass.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


val VoidBg = Color(0xFF080A0E)
val RaisedBg = Color(0xFF1A2130)
val SurfaceBg = Color(0xFF131820)
val BorderColor = Color(0xFF232D3F)
val BorderHi = Color(0xFF2E3D55)
val Muted = Color(0xFF4A5A72)
val Subtle = Color(0xFF7A8FA8)
val Body = Color(0xFFB8C8DC)
val Heading = Color(0xFFE8F0FA)
val White = Color(0xFFF4F8FF)
val Accent = Color(0xFF00E5A0)
val Danger = Color(0xFFFF4D6A)
val Warn = Color(0xFFFFB830)
val Info = Color(0xFF3B9EFF)


val AccentGreen = Accent
val DangerRed = Danger
val WarningYellow = Warn
val InfoBlue = Info
val TextLight = Heading
val TextDim = Subtle
val TextMuted = Muted
val TextVeryMuted = BorderHi
val CardBg = SurfaceBg
val SurfaceDark = RaisedBg
val BorderLight = BorderHi
val StatusStrong = Accent
val StatusModerate = Info
val StatusWeak = Danger

val StatusReused = Warn

val AccentGreenLight = Accent.copy(alpha = 0.15f)
val AccentGreenDim = Accent.copy(alpha = 0.08f)
val DangerRedLight = Danger.copy(alpha = 0.15f)
val WarningYellowLight = Warn.copy(alpha = 0.15f)


private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = VoidBg,
    primaryContainer = RaisedBg,
    onPrimaryContainer = Accent,
    secondary = Info,
    tertiary = Warn,
    background = VoidBg,
    onBackground = Body,
    surface = SurfaceBg,
    onSurface = Heading,
    surfaceVariant = RaisedBg,
    onSurfaceVariant = Subtle,
    error = Danger,
    onError = White,
    errorContainer = RaisedBg,
    onErrorContainer = Danger,
    outline = BorderColor,
    outlineVariant = BorderHi
)

val CipherTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp)
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

        else -> DarkScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CipherTypography,
        content = content
    )
}
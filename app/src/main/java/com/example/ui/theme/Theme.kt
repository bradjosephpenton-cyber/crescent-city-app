package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val StudioColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedBackground,
    surface = SophisticatedSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onPrimary = SophisticatedOnPrimary,
    onSecondary = SophisticatedOnSecondary,
    onTertiary = SophisticatedOnSecondary,
    onBackground = SophisticatedText,
    onSurface = SophisticatedText,
    onSurfaceVariant = SophisticatedSubtext
)

private val LightColorScheme = lightColorScheme(
    primary = SophisticatedPrimary,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedBackground,
    surface = SophisticatedSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onPrimary = SophisticatedOnPrimary,
    onSecondary = SophisticatedOnSecondary,
    onTertiary = SophisticatedOnSecondary,
    onBackground = SophisticatedText,
    onSurface = SophisticatedText,
    onSurfaceVariant = SophisticatedSubtext
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default for premium midnight studio atmosphere
    dynamicColor: Boolean = false, // Disable dynamic colors so our Golden Brass brand identity always shines!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

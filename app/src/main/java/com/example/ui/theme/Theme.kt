package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DtAccent,
    onPrimary = Color.White,
    primaryContainer = DtAccentDim,
    onPrimaryContainer = DtAccentLight,
    secondary = DtAccent,
    onSecondary = Color.White,
    tertiary = DtAccentLight,
    onTertiary = Color.White,
    background = DtBackground,
    onBackground = DtTextPrimary,
    surface = DtSurface,
    onSurface = DtTextPrimary,
    surfaceVariant = DtSurfaceVariant,
    onSurfaceVariant = DtTextSecondary,
    error = DtDanger,
    onError = Color.White,
    outline = DtBorder,
    outlineVariant = DtBorderStrong
)

private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is premium on Android 12+, but for strong cohesive brand identity we can let users experience the custom theme by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

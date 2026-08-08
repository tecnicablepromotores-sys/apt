package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TecnicableDarkPrimary,
    onPrimary = TecnicableDarkOnPrimary,
    secondary = TecnicableStatusAccent,
    onSecondary = TecnicableDarkOnPrimary,
    background = TecnicableDarkBackground,
    onBackground = TecnicableDarkOnBackground,
    surface = TecnicableDarkSurface,
    onSurface = TecnicableDarkOnBackground,
    surfaceVariant = TecnicableDarkSurfaceVariant,
    onSurfaceVariant = TecnicableDarkTextSecondary,
    outline = TecnicableDarkBorder,
    outlineVariant = TecnicableDarkTextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = TecnicablePrimary,
    onPrimary = TecnicableOnPrimary,
    secondary = TecnicableStatusAccent,
    onSecondary = TecnicableOnPrimary,
    background = TecnicableBackground,
    onBackground = TecnicableOnBackground,
    surface = TecnicableSurface,
    onSurface = TecnicableOnBackground,
    surfaceVariant = TecnicableSurfaceVariant,
    onSurfaceVariant = TecnicableTextSecondary,
    outline = TecnicableBorder,
    outlineVariant = TecnicableTextMuted
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> darkTheme
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = TecnicableShapes,
        content = content
    )
}

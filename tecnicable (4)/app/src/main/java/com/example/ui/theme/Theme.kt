package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TecnicableDarkPrimary,
    secondary = TecnicableDarkSecondary,
    background = TecnicableDarkBackground,
    surface = TecnicableDarkSurface,
    onPrimary = TecnicableDarkOnPrimary,
    onSecondary = TecnicableDarkOnPrimary,
    onBackground = TecnicableDarkOnBackground,
    onSurface = TecnicableDarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = TecnicablePrimary,
    secondary = TecnicableSecondary,
    tertiary = TecnicableTertiary,
    background = TecnicableBackground,
    surface = TecnicableSurface,
    onPrimary = TecnicableOnPrimary,
    onSecondary = TecnicableSecondary,
    onBackground = TecnicableOnBackground,
    onSurface = TecnicableOnSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

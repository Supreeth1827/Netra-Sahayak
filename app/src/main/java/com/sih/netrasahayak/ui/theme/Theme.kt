package com.sih.netrasahayak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * One fixed high-contrast light scheme.
 *
 * Dynamic colour and dark mode are intentionally not used: the app must look the
 * same on every phone in the field, and a light scheme stays readable in
 * daylight. [isSystemInDarkTheme] is deliberately ignored.
 */
private val NetraColorScheme = lightColorScheme(
    primary = Teal700,
    onPrimary = Surface0,
    primaryContainer = Teal100,
    onPrimaryContainer = Slate900,
    secondary = Teal600,
    onSecondary = Surface0,
    secondaryContainer = Teal50,
    onSecondaryContainer = Slate900,
    background = Surface0,
    onBackground = Slate900,
    surface = Surface0,
    onSurface = Slate900,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    error = Danger,
    onError = Surface0,
    errorContainer = DangerContainer,
    onErrorContainer = Slate900
)

@Composable
fun NetraSahayakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NetraColorScheme,
        typography = NetraTypography,
        content = content
    )
}

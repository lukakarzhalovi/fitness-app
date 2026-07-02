package com.example.final_exam_project.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = FitTrackPrimary,
    onPrimary = FitTrackOnPrimary,
    primaryContainer = FitTrackPrimaryContainer,
    onPrimaryContainer = FitTrackOnPrimaryContainer,
    secondary = FitTrackSecondary,
    onSecondary = FitTrackOnSecondary,
    secondaryContainer = FitTrackSecondaryContainer,
    onSecondaryContainer = FitTrackOnSecondaryContainer,
    tertiary = FitTrackTertiary,
    onTertiary = FitTrackOnTertiary,
    tertiaryContainer = FitTrackTertiaryContainer,
    onTertiaryContainer = FitTrackOnTertiaryContainer,
    background = FitTrackBackground,
    onBackground = FitTrackOnBackground,
    surface = FitTrackSurface,
    onSurface = FitTrackOnSurface,
    surfaceVariant = FitTrackSurfaceVariant,
    onSurfaceVariant = FitTrackOnSurfaceVariant,
    outline = FitTrackOutline,
    error = FitTrackError,
    onError = FitTrackOnError,
    errorContainer = FitTrackErrorContainer,
    onErrorContainer = FitTrackOnErrorContainer
)

private val DarkColors = darkColorScheme(
    primary = FitTrackDarkPrimary,
    onPrimary = FitTrackDarkOnPrimary,
    primaryContainer = FitTrackDarkPrimaryContainer,
    onPrimaryContainer = FitTrackDarkOnPrimaryContainer,
    secondary = FitTrackDarkSecondary,
    onSecondary = FitTrackDarkOnSecondary,
    secondaryContainer = FitTrackDarkSecondaryContainer,
    onSecondaryContainer = FitTrackDarkOnSecondaryContainer,
    tertiary = FitTrackDarkTertiary,
    onTertiary = FitTrackDarkOnTertiary,
    tertiaryContainer = FitTrackDarkTertiaryContainer,
    onTertiaryContainer = FitTrackDarkOnTertiaryContainer,
    background = FitTrackDarkBackground,
    onBackground = FitTrackDarkOnBackground,
    surface = FitTrackDarkSurface,
    onSurface = FitTrackDarkOnSurface,
    surfaceVariant = FitTrackDarkSurfaceVariant,
    onSurfaceVariant = FitTrackDarkOnSurfaceVariant,
    outline = FitTrackDarkOutline,
    error = FitTrackDarkError,
    onError = FitTrackDarkOnError,
    errorContainer = FitTrackDarkErrorContainer,
    onErrorContainer = FitTrackDarkOnErrorContainer
)

@Composable
fun FitTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FitTrackTypography,
        content = content
    )
}

package com.glyphora.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.glyphora.domain.model.ReaderThemeMode

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    secondary = SecondaryTeal,
    onSecondary = OnSecondaryTeal
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryContainerBlue,
    onPrimary = OnPrimaryContainerBlue,
    background = DarkReaderBackground,
    surface = DarkReaderSurface,
    onBackground = DarkReaderOnSurface,
    onSurface = DarkReaderOnSurface
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaOnSurface,
    onPrimary = SepiaBackground,
    background = SepiaBackground,
    surface = SepiaSurface,
    onBackground = SepiaOnSurface,
    onSurface = SepiaOnSurface
)

@Composable
fun GlyphoraTheme(
    themeMode: ReaderThemeMode = ReaderThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when (themeMode) {
        ReaderThemeMode.SEPIA -> SepiaColorScheme
        ReaderThemeMode.LIGHT -> LightColorScheme
        ReaderThemeMode.DARK -> DarkColorScheme
        ReaderThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemInDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemInDark) DarkColorScheme else LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GlyphoraTypography,
        content = content
    )
}

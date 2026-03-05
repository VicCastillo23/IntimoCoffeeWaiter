package com.intimocoffee.waiter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WhiteOff,
    onPrimary = BlackPrimary,
    primaryContainer = GrayMedium,
    onPrimaryContainer = GrayLightest,
    secondary = GrayLighter,
    onSecondary = GrayDark,
    secondaryContainer = GrayLight,
    onSecondaryContainer = WhiteOff,
    tertiary = GrayLightest,
    onTertiary = GrayDark,
    tertiaryContainer = GrayMedium,
    onTertiaryContainer = WhiteOff,
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
    onError = OnErrorDark,
    onErrorContainer = OnErrorContainerDark,
    background = BlackPrimary,
    onBackground = WhiteOff,
    surface = GrayDark,
    onSurface = WhiteOff,
    surfaceVariant = GrayMedium,
    onSurfaceVariant = GrayLightest,
    outline = GrayLight,
    inverseOnSurface = GrayDark,
    inverseSurface = WhiteOff,
    inversePrimary = GrayMedium,
)

private val LightColorScheme = lightColorScheme(
    primary = BlackPrimary,
    onPrimary = WhitePure,
    primaryContainer = GrayMedium,
    onPrimaryContainer = WhiteOff,
    secondary = GrayLight,
    onSecondary = WhitePure,
    secondaryContainer = GrayLighter,
    onSecondaryContainer = BlackPrimary,
    tertiary = GrayMedium,
    onTertiary = WhitePure,
    tertiaryContainer = GrayLightest,
    onTertiaryContainer = BlackPrimary,
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
    onError = WhitePure,
    onErrorContainer = OnErrorContainerLight,
    background = WhiteOff,
    onBackground = BlackPrimary,
    surface = WhitePure,
    onSurface = BlackPrimary,
    surfaceVariant = GrayLightest,
    onSurfaceVariant = GrayMedium,
    outline = GrayLight,
    inverseOnSurface = WhiteOff,
    inverseSurface = GrayDark,
    inversePrimary = GrayLightest,
)

@Composable
fun IntimoCoffeeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to maintain our monochromatic theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
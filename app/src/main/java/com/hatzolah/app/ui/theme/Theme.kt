package com.hatzolah.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val HatzolahBlue = Color(0xFF1565C0)
private val HatzolahDarkBlue = Color(0xFF0D47A1)
private val HatzolahLightBlue = Color(0xFF42A5F5)
private val HatzolahRed = Color(0xFFD32F2F)
private val HatzolahWhite = Color(0xFFFFFFFF)
private val HatzolahGray = Color(0xFFF5F5F5)

private val LightColorScheme = lightColorScheme(
    primary = HatzolahBlue,
    onPrimary = HatzolahWhite,
    primaryContainer = HatzolahLightBlue,
    secondary = HatzolahRed,
    onSecondary = HatzolahWhite,
    background = HatzolahWhite,
    surface = HatzolahGray,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = HatzolahLightBlue,
    onPrimary = HatzolahDarkBlue,
    primaryContainer = HatzolahDarkBlue,
    secondary = Color(0xFFEF5350),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun HatzolahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

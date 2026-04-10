package com.hatzolah.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Hatzolah brand colors
val HatzolahRed = Color(0xFFCC1B1B)
val HatzolahDarkRed = Color(0xFF8B0000)
val HatzolahBlue = Color(0xFF0D47A1)
val HatzolahDarkBlue = Color(0xFF002171)
val HatzolahLightBlue = Color(0xFF5472D3)
val HatzolahGold = Color(0xFFFFB300)
val HatzolahWhite = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = HatzolahBlue,
    onPrimary = HatzolahWhite,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = HatzolahDarkBlue,
    secondary = HatzolahRed,
    onSecondary = HatzolahWhite,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = HatzolahDarkRed,
    tertiary = HatzolahGold,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFE082),
    onTertiaryContainer = Color(0xFF3E2D00),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = HatzolahWhite,
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE7E8F0),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    error = Color(0xFFBA1A1A),
    onError = HatzolahWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2E3036),
    inverseOnSurface = Color(0xFFF0F0F7)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = HatzolahBlue,
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF690005),
    secondaryContainer = HatzolahDarkRed,
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFFFCC02),
    onTertiary = Color(0xFF3E2D00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE082),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE2E2E9),
    inverseOnSurface = Color(0xFF2E3036)
)

@Composable
fun HatzolahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our brand colors by default
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

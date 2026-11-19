package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val ProQuoteShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

fun parseBrandColor(hex: String?, fallback: Color = Slate900): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.trim().removePrefix("#")
        val parsed = when (clean.length) {
            6 -> ("FF$clean").toLong(16)
            8 -> clean.toLong(16)
            else -> return fallback
        }
        Color(parsed)
    } catch (_: Exception) {
        fallback
    }
}

fun contrastOnColor(bg: Color): Color {
    return if (bg.luminance() > 0.45f) Slate900 else Color.White
}

@Composable
fun ProQuoteTheme(
    brandColorHex: String? = null,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(brandColorHex, darkTheme) {
        if (darkTheme) {
            darkColorScheme(
                primary = Emerald500,
                onPrimary = Slate950,
                primaryContainer = Slate800,
                onPrimaryContainer = Slate100,
                secondary = Cobalt600,
                onSecondary = Color.White,
                background = Slate950,
                onBackground = Slate50,
                surface = Slate900,
                onSurface = Slate50,
                surfaceVariant = Slate800,
                onSurfaceVariant = Slate400,
                outline = Slate700,
                outlineVariant = Slate600,
                error = Coral600,
                onError = Color.White
            )
        } else {
            val primaryColor = parseBrandColor(brandColorHex, Slate900)
            val onPrimaryColor = contrastOnColor(primaryColor)
            lightColorScheme(
                primary = primaryColor,
                onPrimary = onPrimaryColor,
                primaryContainer = Slate100,
                onPrimaryContainer = Slate900,
                secondary = Emerald600,
                onSecondary = Color.White,
                secondaryContainer = Emerald50,
                onSecondaryContainer = Emerald600,
                tertiary = Cobalt600,
                onTertiary = Color.White,
                background = Slate50,
                onBackground = Slate900,
                surface = Color.White,
                onSurface = Slate900,
                surfaceVariant = Slate100,
                onSurfaceVariant = Slate600,
                outline = Slate200,
                outlineVariant = Slate300,
                error = Coral600,
                onError = Color.White,
                errorContainer = Coral50,
                onErrorContainer = Coral600
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ProQuoteTypography,
        shapes = ProQuoteShapes,
        content = content
    )
}

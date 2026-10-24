package com.mehran.farsiplex.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Palette taken from the FarsiPlex player accent (#0B7EF4) on a near-black base. */
val Bg0 = Color(0xFF07080C)
val Bg1 = Color(0xFF0E1118)
val Surface1 = Color(0xFF141923)
val Surface2 = Color(0xFF1D2432)
val Brand = Color(0xFF0B7EF4)
val BrandSoft = Color(0xFF4FA6FF)
val Text1 = Color(0xFFF3F6FF)
val Text2 = Color(0xFF98A4BC)
val Warning = Color(0xFFFFB4AB)
val Gold = Color(0xFFFFC857)

private val Scheme = darkColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    secondary = BrandSoft,
    background = Bg0,
    onBackground = Text1,
    surface = Bg1,
    onSurface = Text1,
    error = Warning
)

@Composable
fun MehranFarsiPlexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(18.dp)
        ),
        content = content
    )
}

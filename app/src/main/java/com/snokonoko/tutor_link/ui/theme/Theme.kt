package com.snokonoko.tutor_link.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// The demo's near-square editorial corner (4dp) and the large-card corner (8dp).
val R4 = RoundedCornerShape(4.dp)
val R8 = RoundedCornerShape(8.dp)

private val Colors = lightColorScheme(
    primary = TL.Navy,
    onPrimary = TL.Gold,
    secondary = TL.Gold,
    onSecondary = TL.Navy,
    background = TL.Bg,
    onBackground = TL.Text,
    surface = TL.Card,
    onSurface = TL.Text,
    surfaceContainerLow = TL.Card,
    outline = TL.Line,
    error = TL.Red,
)

// Light theme only, same as the demo.
@Composable
fun TutorLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = Typography,
        shapes = Shapes(small = R4, medium = R4, large = R8),
        content = content,
    )
}

package com.snokonoko.tutor_link.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.snokonoko.tutor_link.ui.theme.TL

// The demo's hand-drawn icons (the P object in app.js), drawn as 24x24 stroked paths.
private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r},${y}h${w - 2 * r}a$r,$r 0 0 1 $r,${r}v${h - 2 * r}a$r,$r 0 0 1 -$r,${r}h-${w - 2 * r}" +
        "a$r,$r 0 0 1 -$r,-${r}v-${h - 2 * r}a$r,$r 0 0 1 $r,-${r}z"

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r},${cy}a$r,$r 0 1 0 ${2 * r},0a$r,$r 0 1 0 -${2 * r},0z"

enum class Ic(vararg val paths: String) {
    Back("M15 18l-6-6 6-6"),
    Chev("M9 18l6-6-6-6"),
    Mail(rect(3f, 5f, 18f, 14f, 2f), "M3.6 7.5l7.3 5.2a2 2 0 0 0 2.2 0l7.3-5.2"),
    Inbox(
        "M3 13.5V7a2.5 2.5 0 0 1 2.5-2.5h13A2.5 2.5 0 0 1 21 7v6.5",
        "M3 13.5h4.6l1.4 2.6h6l1.4-2.6H21v3.9a2.6 2.6 0 0 1-2.6 2.6H5.6A2.6 2.6 0 0 1 3 17.4z",
    ),
    Cap("M2.5 8.5L12 4l9.5 4.5L12 13z", "M6.5 10.8V16c0 1.4 2.5 2.6 5.5 2.6s5.5-1.2 5.5-2.6v-5.2", "M21 9v5"),
    Board(rect(3f, 4f, 18f, 13f, 2f), "M12 17v3M8.5 20h7M8 9.5l2.2 2.2L16 8"),
    Shield("M12 3l7.5 3v5.4c0 4.3-3 8.1-7.5 9.6-4.5-1.5-7.5-5.3-7.5-9.6V6z", "M9.3 12l1.9 1.9 3.6-3.8"),
    People(
        circle(9f, 8.5f, 3.3f), "M2.8 19.2c.5-3.1 3.1-5 6.2-5s5.7 1.9 6.2 5",
        "M16.2 6.2a3.2 3.2 0 0 1 0 6.1", "M17.6 14.6c2 .6 3.4 2.2 3.7 4.6",
    ),
    User(circle(12f, 8.4f, 3.7f), "M4.6 20c.6-3.7 3.7-6 7.4-6s6.8 2.3 7.4 6"),
    Cal(rect(3f, 5f, 18f, 16f, 2f), "M3 10h18M8 3v4M16 3v4"),
    CalChk(rect(3f, 5f, 18f, 16f, 2f), "M3 10h18M8 3v4M16 3v4M9 15l2 2 4-4"),
    Clip(
        "M16 4h1.5A2.5 2.5 0 0 1 20 6.5v12A2.5 2.5 0 0 1 17.5 21h-11A2.5 2.5 0 0 1 4 18.5v-12A2.5 2.5 0 0 1 6.5 4H8",
        rect(8f, 2.5f, 8f, 3.6f, 1.2f), "M8.5 13l1.8 1.8 3.5-3.6",
    ),
    Wallet("M3 8.5A2.5 2.5 0 0 1 5.5 6H18a2 2 0 0 1 2 2v1", rect(3f, 8.5f, 18f, 11f, 2.5f), "M16.5 14h1.2"),
    Doc("M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z", "M14 3v5h5M9 13h6M9 17h4"),
    Upload(
        "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z", "M14 3v5h5",
        "M12 18v-6M9.5 14.5L12 12l2.5 2.5",
    ),
    Plus("M12 5v14M5 12h14"),
    Check("M4 12.5l5 5L20 6.5"),
    X("M18 6L6 18M6 6l12 12"),
    Alert("M10.3 3.9L2 18a2 2 0 0 0 1.7 3h16.6a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z", "M12 9.5v4M12 17h.01"),
    Clock(circle(12f, 12f, 9f), "M12 7v5.2l3.2 1.9"),
    Sliders("M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1.5 14h5M9.5 8h5M17.5 16h5"),
    Down("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M7.5 10.5L12 15l4.5-4.5M12 15V3"),
    Pin("M12 21s7-5.6 7-11a7 7 0 1 0-14 0c0 5.4 7 11 7 11z", circle(12f, 10f, 2.6f)),
    Help(circle(12f, 12f, 9f), "M9.6 9.3a2.5 2.5 0 0 1 4.8.9c0 1.7-2.4 2.3-2.4 3.6M12 17h.01"),
}

private val cache = HashMap<Pair<Ic, Float>, ImageVector>()

private fun vector(icon: Ic, stroke: Float): ImageVector = cache.getOrPut(icon to stroke) {
    val b = ImageVector.Builder(name = icon.name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    icon.paths.forEach { d ->
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    b.build()
}

@Composable
fun TLIcon(icon: Ic, size: Int = 22, stroke: Float = 1.7f, tint: Color = TL.Navy, modifier: Modifier = Modifier) {
    val v = remember(icon, stroke) { vector(icon, stroke) }
    Icon(imageVector = v, contentDescription = null, tint = tint, modifier = modifier.size(size.dp))
}

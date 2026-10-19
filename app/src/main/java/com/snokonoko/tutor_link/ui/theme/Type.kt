package com.snokonoko.tutor_link.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.R

// Cormorant Garamond for headings and big figures, DM Sans for body text, monospace for codes.
val Serif = FontFamily(
    Font(R.font.cormorant_garamond_600, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_700, FontWeight.Bold),
)
val Sans = FontFamily(
    Font(R.font.dm_sans_400, FontWeight.Normal),
    Font(R.font.dm_sans_500, FontWeight.Medium),
    Font(R.font.dm_sans_600, FontWeight.SemiBold),
    Font(R.font.dm_sans_700, FontWeight.Bold),
)
val Mono = FontFamily.Monospace

object TLType {
    val display = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em, color = TL.Text)
    val displaySm = display.copy(fontSize = 30.sp, lineHeight = 32.sp)
    val sub = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 23.sp, color = TL.Muted)
    val eyebrow = TextStyle(fontFamily = Mono, fontSize = 10.sp, letterSpacing = 0.16.em, color = TL.Muted2)
    val sect = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.12.em, color = TL.Muted)
    val navTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.01.em, color = TL.Text)
    val rname = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, letterSpacing = (-0.01).em, color = TL.Text)
    val rmeta = TextStyle(fontFamily = Sans, fontSize = 12.sp, color = TL.Muted)
    val stamp = TextStyle(fontFamily = Mono, fontSize = 10.5.sp, color = TL.Muted2)
    val small = TextStyle(fontFamily = Sans, fontSize = 12.5.sp, lineHeight = 20.sp, color = TL.Muted)
    val body = TextStyle(fontFamily = Sans, fontSize = 13.5.sp, lineHeight = 23.sp, color = TL.BodyText)
    val label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.1.em, color = TL.Muted)
    val input = TextStyle(fontFamily = Sans, fontSize = 16.sp, color = TL.Text)
    val button = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.04.em)
    val badge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.07.em)
    val metric = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 27.sp, color = TL.Text)
    val money = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 42.sp, color = TL.GoldD)
    val sheetTitle = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, color = TL.Text)
    val emptyTitle = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TL.Text)
    val tab = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp, letterSpacing = 0.04.em)
}

val Typography = Typography(
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, color = TL.Text),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, color = TL.Text),
    labelLarge = TLType.button,
)

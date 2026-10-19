package com.snokonoko.tutor_link.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.data.ModuleDto
import com.snokonoko.tutor_link.ui.theme.Mono
import com.snokonoko.tutor_link.ui.theme.R4
import com.snokonoko.tutor_link.ui.theme.Sans
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TLType
import kotlinx.coroutines.delay

fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() && it[0].isLetter() }.let { parts ->
        when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2)
            else -> "${parts.first()[0]}${parts.last()[0]}"
        }
    }.uppercase()

/** Draws a 3dp coloured strip down the left edge, like the demo's border-left accents. */
fun Modifier.leftEdge(color: Color, width: Dp = 3.dp) = this.drawBehind {
    drawRect(color, size = Size(width.toPx(), size.height))
}

/* ---------- top bar ---------- */

@Composable
fun NavBar(
    title: String,
    left: (@Composable () -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().background(TL.Bg.copy(alpha = 0.94f)).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 5.dp).height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(46.dp), contentAlignment = Alignment.CenterStart) { left?.invoke() }
            Text(title, style = TLType.navTitle, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Box(Modifier.width(46.dp), contentAlignment = Alignment.CenterEnd) { right?.invoke() }
        }
        HorizontalDivider(color = TL.Line, thickness = 1.dp)
    }
}

@Composable
fun IconBtn(icon: Ic, onClick: () -> Unit, size: Int = 22, stroke: Float = 1.7f, count: Int = 0, label: String? = null) {
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    Box(
        Modifier
            .size(44.dp)
            .pressSpring(src, 0.9f)
            .clip(R4)
            .clickable(interactionSource = src, indication = null) { haptics.buzz(Buzz.TAB); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        TLIcon(icon, size, stroke)
        if (count > 0) CountBubble(count, Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 4.dp))
    }
}

@Composable
fun CountBubble(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
            .border(2.dp, TL.Bg, RoundedCornerShape(9.dp))
            .clip(RoundedCornerShape(9.dp))
            .background(TL.Gold)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), fontFamily = Mono, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TL.Navy)
    }
}

/* ---------- text ---------- */

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), style = TLType.eyebrow, modifier = modifier.padding(bottom = 10.dp))

@Composable
fun Display(text: String, small: Boolean = false, modifier: Modifier = Modifier) =
    Text(text, style = if (small) TLType.displaySm else TLType.display, modifier = modifier)

@Composable
fun Sub(text: String, modifier: Modifier = Modifier) =
    Text(text, style = TLType.sub, modifier = modifier.padding(top = 10.dp))

@Composable
fun Sect(title: String, modifier: Modifier = Modifier, first: Boolean = false, action: Pair<String, () -> Unit>? = null) {
    Column(modifier.fillMaxWidth().padding(top = if (first) 0.dp else 26.dp, bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
            Text(title.uppercase(), style = TLType.sect, modifier = Modifier.weight(1f))
            if (action != null) {
                Text(
                    action.first.uppercase(),
                    style = TLType.sect.copy(color = TL.GoldD),
                    modifier = Modifier.clickable { action.second() },
                )
            }
        }
        HorizontalDivider(color = TL.Line, thickness = 1.dp)
    }
}

/* ---------- surfaces ---------- */

@Composable
fun TLCard(
    modifier: Modifier = Modifier,
    edge: Color? = null,
    pad: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(R4)
            .background(TL.Card)
            .border(1.dp, TL.Line, R4)
            .then(if (edge != null) Modifier.leftEdge(edge) else Modifier)
            .then(if (pad) Modifier.padding(horizontal = 16.dp, vertical = 18.dp) else Modifier),
        content = content,
    )
}

@Composable
fun RowDivider() = HorizontalDivider(color = TL.LineS, thickness = 1.dp)

/** One row in a card: optional leading avatar, a name and meta line, and a trailing slot. */
@Composable
fun ListRow(
    title: String,
    meta: String? = null,
    onClick: (() -> Unit)? = null,
    metaMono: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = src, indication = androidx.compose.material3.ripple(color = TL.Muted)) {
                    haptics.buzz(Buzz.TAP); onClick()
                } else Modifier,
            )
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = TLType.rname, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (meta != null) {
                Text(
                    meta,
                    style = if (metaMono) TLType.rmeta.copy(fontFamily = Mono) else TLType.rmeta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = trailing)
        }
        if (onClick != null) TLIcon(Ic.Chev, 17, 2.1f, TL.Muted2)
    }
}

enum class AvatarKind { Navy, Gold, Code }

@Composable
fun Avatar(text: String, kind: AvatarKind = AvatarKind.Navy, large: Boolean = false) {
    val (bg, fg) = when (kind) {
        AvatarKind.Navy -> TL.Navy to TL.Gold
        AvatarKind.Gold -> TL.GoldBg to TL.GoldD
        AvatarKind.Code -> TL.Bg to TL.Navy
    }
    val border = when (kind) {
        AvatarKind.Navy -> null
        AvatarKind.Gold -> TL.GoldBd
        AvatarKind.Code -> TL.Line
    }
    Box(
        Modifier
            .size(if (large) 52.dp else 40.dp)
            .clip(R4)
            .background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, R4) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = fg,
            fontFamily = if (kind == AvatarKind.Code) Mono else Sans,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (kind == AvatarKind.Code) 9.5.sp else if (large) 16.sp else 13.sp,
        )
    }
}

enum class BadgeKind(val bg: Color, val fg: Color, val bd: Color) {
    Green(TL.GreenBg, TL.Green, TL.GreenBd),
    Red(TL.RedBg, TL.Red, TL.RedBd),
    Amber(TL.AmberBg, TL.Amber, TL.AmberBd),
    Slate(TL.SlateBg, TL.Slate, TL.SlateBd),
    Gold(TL.GoldBg, TL.GoldD, TL.GoldBd),
    Plain(TL.Bg, TL.Muted, TL.Line),
}

@Composable
fun Badge(text: String, kind: BadgeKind) {
    Text(
        text.uppercase(),
        style = TLType.badge.copy(color = kind.fg),
        maxLines = 1,
        modifier = Modifier
            .clip(R4)
            .background(kind.bg)
            .border(1.dp, kind.bd, R4)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/* ---------- chips ---------- */

@Composable
fun ModuleChip(code: String, name: String, on: Boolean = false, small: Boolean = false, onClick: (() -> Unit)? = null) {
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .then(if (onClick != null) Modifier.pressSpring(src, 0.94f) else Modifier)
            .clip(shape)
            .background(if (on) TL.Navy else TL.Card)
            .border(1.dp, if (on) TL.Navy else TL.Line, shape)
            .then(if (onClick != null) Modifier.clickable(interactionSource = src, indication = null) { haptics.buzz(7); onClick() } else Modifier)
            .heightIn(min = if (small) 0.dp else 38.dp)
            .padding(horizontal = if (small) 11.dp else 13.dp, vertical = if (small) 6.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(code, fontFamily = Mono, fontSize = 10.sp, color = if (on) TL.Gold else TL.Muted)
        Text(name, fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = if (small) 12.sp else 13.sp, color = if (on) Color.White else TL.Text)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModuleChips(modules: List<ModuleDto>, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        modules.forEach { ModuleChip(it.code, it.name, small = true) }
    }
}

@Composable
fun FilterChip(text: String, on: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    val shape = RoundedCornerShape(999.dp)
    Text(
        text,
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = if (on) Color.White else TL.Text,
        modifier = Modifier
            .pressSpring(src, 0.94f)
            .clip(shape)
            .background(if (on) TL.Navy else TL.Card)
            .border(1.dp, if (on) TL.Navy else TL.Line, shape)
            .clickable(interactionSource = src, indication = null) { haptics.buzz(Buzz.TAB); onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
    )
}

/* ---------- metrics, profile, notes ---------- */

enum class MetricKind(val edge: Color, val value: Color) {
    Plain(TL.Line, TL.Text),
    Navy(TL.Navy, TL.Text),
    Gold(TL.Gold, TL.GoldD),
    Green(TL.Green, TL.Green),
    Amber(TL.Amber, TL.Amber),
}

@Composable
fun Metric(value: String, label: String, kind: MetricKind, modifier: Modifier = Modifier, small: Boolean = false) {
    Column(
        modifier
            .clip(R4)
            .background(TL.Card)
            .border(1.dp, TL.Line, R4)
            .leftEdge(kind.edge)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(value, style = TLType.metric.copy(color = kind.value, fontSize = if (small) 21.sp else 27.sp), maxLines = 1)
        Text(label, style = TLType.rmeta.copy(fontSize = 10.5.sp), modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun MetricsRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) =
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)

@Composable
fun ProfileCard(name: String, line: String, gold: Boolean = false) {
    TLCard(pad = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(initials(name), if (gold) AvatarKind.Gold else AvatarKind.Navy, large = true)
            Column {
                Text(name, style = TLType.rname.copy(fontSize = 16.sp))
                Text(line, style = TLType.rmeta, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

enum class NoteKind(val bg: Color, val bd: Color, val fg: Color) {
    Plain(TL.Bg, TL.Line, TL.Muted),
    Gold(TL.GoldBg, TL.GoldBd, TL.GoldText),
    Green(TL.GreenBg, TL.GreenBd, TL.Green),
    Red(TL.RedBg, TL.RedBd, TL.Red),
    Amber(TL.AmberBg, TL.AmberBd, TL.Amber),
}

@Composable
fun Note(text: AnnotatedString, kind: NoteKind = NoteKind.Plain, icon: Ic = Ic.Alert, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(R4)
            .background(kind.bg)
            .border(1.dp, kind.bd, R4)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TLIcon(icon, 15, tint = kind.fg, modifier = Modifier.padding(top = 2.dp))
        Text(text, fontFamily = Sans, fontSize = 12.5.sp, lineHeight = 19.sp, color = kind.fg)
    }
}

@Composable
fun Note(text: String, kind: NoteKind = NoteKind.Plain, icon: Ic = Ic.Alert, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) =
    Note(AnnotatedString(text), kind, icon, modifier, onClick)

/* ---------- buttons ---------- */

enum class BtnStyle { Gold, Navy, Outline, Ghost, Danger }

@Composable
fun TLButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.Gold,
    enabled: Boolean = true,
    loading: Boolean = false,
    small: Boolean = false,
    icon: Ic? = null,
    buzz: Long = Buzz.TAP,
) {
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    val (bg, fg, border) = when (style) {
        BtnStyle.Gold -> Triple(TL.Gold, TL.Navy, null)
        BtnStyle.Navy -> Triple(TL.Navy, TL.Gold, null)
        BtnStyle.Outline -> Triple(Color.Transparent, TL.Text, TL.Line)
        BtnStyle.Ghost -> Triple(Color.Transparent, TL.Muted, null)
        BtnStyle.Danger -> Triple(TL.RedBg, TL.Red, TL.RedBd)
    }
    val active = enabled && !loading
    Row(
        modifier
            .then(if (small) Modifier else Modifier.fillMaxWidth())
            .then(if (active) Modifier.pressSpring(src) else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(R4)
            .background(bg)
            .then(if (border != null) Modifier.border(BorderStroke(1.5.dp, SolidColor(border)), R4) else Modifier)
            .clickable(interactionSource = src, indication = null, enabled = active) { haptics.buzz(buzz); onClick() }
            .heightIn(min = if (small) 38.dp else 50.dp)
            .padding(horizontal = if (small) 14.dp else 18.dp, vertical = if (small) 9.dp else 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            if (icon != null) {
                TLIcon(icon, 16, tint = fg)
                Spacer(Modifier.width(7.dp))
            }
            Text(text, style = TLType.button.copy(color = fg, fontSize = if (small) 12.sp else 14.sp))
        }
    }
}

/* ---------- form field ---------- */

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    ime: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    capitalize: Boolean = false,
    minLines: Int = 1,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = TLType.label, modifier = Modifier.padding(start = 1.dp, bottom = 6.dp))
        val borderColor = when {
            error != null -> TL.Red
            focused -> TL.Gold
            else -> TL.Line
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TLType.input.copy(fontSize = if (minLines > 1) 15.sp else 16.sp, lineHeight = if (minLines > 1) 23.sp else 20.sp),
            singleLine = minLines == 1,
            minLines = minLines,
            cursorBrush = SolidColor(TL.Navy),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else keyboard,
                imeAction = ime,
                capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }, onGo = { onDone?.invoke() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .clip(R4)
                .background(TL.Card)
                .border(1.5.dp, borderColor, R4)
                .padding(13.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = TLType.input.copy(color = TL.Muted2, fontSize = if (minLines > 1) 15.sp else 16.sp))
                    inner()
                }
            },
        )
        if (error != null) {
            Text(error, fontFamily = Sans, fontSize = 12.sp, color = TL.Red, modifier = Modifier.padding(top = 5.dp, start = 1.dp))
        }
    }
}

/* ---------- empty state + loading ---------- */

@Composable
fun EmptyState(title: String, line: String, icon: Ic = Ic.Inbox, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(R4).background(TL.Card).border(1.dp, TL.Line, R4),
            contentAlignment = Alignment.Center,
        ) { TLIcon(icon, 24, tint = TL.Muted2) }
        Text(title, style = TLType.emptyTitle, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
        Text(line, style = TLType.small.copy(fontSize = 13.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
        if (action != null) Box(Modifier.padding(top = 20.dp)) { action() }
    }
}

@Composable
fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TL.Gold, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit) {
    EmptyState("Couldn't load this", message, Ic.Alert) {
        TLButton("Try again", onRetry, style = BtnStyle.Outline, small = true)
    }
}

@Composable
fun UnreadDot(modifier: Modifier = Modifier) =
    Box(modifier.size(8.dp).clip(CircleShape).background(TL.Gold))

/* ---------- list fade-in (the demo's .stagger) ---------- */

@Composable
fun Rise(index: Int, content: @Composable () -> Unit) {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            delay(50L * index.coerceAtMost(10))
            progress.animateTo(1f, tween(500))
        }
    }
    Box(Modifier.graphicsLayer { alpha = progress.value; translationY = (1f - progress.value) * 12.dp.toPx() }) { content() }
}

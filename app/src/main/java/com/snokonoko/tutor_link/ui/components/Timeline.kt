package com.snokonoko.tutor_link.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.data.MessageDto
import com.snokonoko.tutor_link.domain.Dot
import com.snokonoko.tutor_link.domain.MessageLogic
import com.snokonoko.tutor_link.ui.theme.Mono
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TLType

/**
 * A tutor's thread as a timeline, not a chat: a line down the left and a dot per notice.
 * Tapping one unfolds it; for the tutor, that also marks it read ([onOpen]).
 */
@Composable
fun MessageTimeline(
    messages: List<MessageDto>,
    myId: String,
    viewerIsTutor: Boolean,
    previews: Boolean,
    onOpen: (MessageDto) -> Unit,
) {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = 4.5.dp.toPx()
                drawLine(TL.Line, Offset(x, 10.dp.toPx()), Offset(x, size.height - 10.dp.toPx()), strokeWidth = 1.dp.toPx())
            }
            .padding(start = 24.dp),
    ) {
        messages.forEachIndexed { i, m ->
            val open = openId == m.id
            Rise(i) {
                TimelineItem(m, MessageLogic.dot(m, myId, viewerIsTutor), MessageLogic.delivered(m, myId), open, previews, last = i == messages.lastIndex) {
                    openId = if (open) null else m.id
                    if (!open) onOpen(m)
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(m: MessageDto, dot: Dot, delivered: Boolean, open: Boolean, previews: Boolean, last: Boolean, onTap: () -> Unit) {
    val haptics = LocalHaptics.current
    val reduce = LocalReduceMotion.current
    val src = remember { MutableInteractionSource() }
    val dotScale by animateFloatAsState(
        targetValue = when {
            open -> 1.3f
            dot == Dot.Unread -> 1.22f
            else -> 1f
        },
        animationSpec = if (reduce) tween(1) else spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "dot",
    )
    val fill by animateColorAsState(
        when (dot) { Dot.Unread -> TL.Gold; Dot.Mine -> TL.Navy; Dot.Read -> TL.Bg },
        animationSpec = tween(if (reduce) 1 else 300), label = "dotFill",
    )
    val ring by animateColorAsState(
        when (dot) { Dot.Unread -> TL.Gold; Dot.Mine -> TL.Navy; Dot.Read -> TL.Line },
        animationSpec = tween(if (reduce) 1 else 300), label = "dotRing",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = src, indication = null) { haptics.buzz(5); onTap() }
            .drawBehind {
                if (!last) drawLine(TL.LineS, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            },
    ) {
        // the dot sits on the line, 24dp to the left of the text
        Box(
            Modifier
                .offset(x = (-24).dp, y = 21.dp)
                .size(9.dp)
                .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                .clip(CircleShape)
                .background(fill)
                .border(2.dp, ring, CircleShape),
        )
        Column(Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    m.topic,
                    style = TLType.rname.copy(color = if (open) TL.Navy else TL.Text),
                    modifier = Modifier.weight(1f),
                )
                Text(MessageLogic.ago(m.sentAt), style = TLType.stamp, modifier = Modifier.padding(top = 2.dp))
            }
            Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                when (dot) {
                    Dot.Mine -> RoleTag("You", TL.Bg, TL.Muted, TL.Line)
                    else -> if (m.senderRole == "lecturer") RoleTag("Lecturer", TL.GoldBg, TL.GoldD, null)
                    else RoleTag("Admin", TL.BlueBg, TL.Blue, null)
                }
                if (dot != Dot.Mine) Text(m.senderName, style = TLType.rmeta)
                if (delivered) Text("· delivered", style = TLType.stamp)
            }
            if (previews && !open) {
                Text(
                    m.body,
                    style = TLType.rmeta.copy(fontSize = 12.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            AnimatedVisibility(
                visible = open,
                enter = if (reduce) fadeIn(tween(1)) else expandVertically(tween(440)) + fadeIn(tween(300)),
                exit = if (reduce) fadeOut(tween(1)) else shrinkVertically(tween(440)) + fadeOut(tween(200)),
            ) {
                Column(Modifier.padding(top = 10.dp)) {
                    Text(m.body, style = TLType.body)
                    Text(MessageLogic.fullTime(m.sentAt), style = TLType.stamp, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun RoleTag(text: String, bg: Color, fg: Color, border: Color?) {
    val shape = RoundedCornerShape(3.dp)
    Text(
        text.uppercase(),
        fontFamily = Mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        letterSpacing = 0.08.sp * 9.5f,
        color = fg,
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** The navy floating button from the demo (.fab). */
@Composable
fun Fab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    Row(
        modifier
            .pressSpring(src, 0.94f)
            .clip(com.snokonoko.tutor_link.ui.theme.R4)
            .background(TL.Navy)
            .clickable(interactionSource = src, indication = null) { haptics.buzz(Buzz.TAP); onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TLIcon(Ic.Plus, 18, 2.3f, TL.Gold)
        Text(text, style = TLType.button.copy(color = TL.Gold, fontSize = 13.5.sp))
    }
}

package com.snokonoko.tutor_link.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.data.AppDetail
import com.snokonoko.tutor_link.data.ModuleDto
import com.snokonoko.tutor_link.data.moduleName
import com.snokonoko.tutor_link.domain.PayMath
import com.snokonoko.tutor_link.ui.components.Avatar
import com.snokonoko.tutor_link.ui.components.AvatarKind
import com.snokonoko.tutor_link.ui.components.Badge
import com.snokonoko.tutor_link.ui.components.BadgeKind
import com.snokonoko.tutor_link.ui.components.BtnStyle
import com.snokonoko.tutor_link.ui.components.Buzz
import com.snokonoko.tutor_link.ui.components.EmptyState
import com.snokonoko.tutor_link.ui.components.Fab
import com.snokonoko.tutor_link.ui.components.Field
import com.snokonoko.tutor_link.ui.components.Ic
import com.snokonoko.tutor_link.ui.components.IconBtn
import com.snokonoko.tutor_link.ui.components.ListRow
import com.snokonoko.tutor_link.ui.components.LocalHaptics
import com.snokonoko.tutor_link.ui.components.MessageTimeline
import com.snokonoko.tutor_link.ui.components.Metric
import com.snokonoko.tutor_link.ui.components.MetricKind
import com.snokonoko.tutor_link.ui.components.MetricsRow
import com.snokonoko.tutor_link.ui.components.ModuleChip
import com.snokonoko.tutor_link.ui.components.ModuleChips
import com.snokonoko.tutor_link.ui.components.NavBar
import com.snokonoko.tutor_link.ui.components.Note
import com.snokonoko.tutor_link.ui.components.NoteKind
import com.snokonoko.tutor_link.ui.components.Rise
import com.snokonoko.tutor_link.ui.components.RowDivider
import com.snokonoko.tutor_link.ui.components.Sect
import com.snokonoko.tutor_link.ui.components.TLButton
import com.snokonoko.tutor_link.ui.components.TLCard
import com.snokonoko.tutor_link.ui.components.TLIcon
import com.snokonoko.tutor_link.ui.components.initials
import com.snokonoko.tutor_link.ui.screens.formatDate
import com.snokonoko.tutor_link.ui.theme.Mono
import com.snokonoko.tutor_link.ui.theme.R4
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TLType
import com.snokonoko.tutor_link.ui.tutor.LogSessionSheet
import com.snokonoko.tutor_link.ui.tutor.Loadable

@Composable
private fun DetailPage(
    title: String,
    onBack: () -> Unit,
    right: (@Composable () -> Unit)? = null,
    fab: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(TL.Bg)) {
        Column(Modifier.fillMaxSize()) {
            NavBar(title, left = { IconBtn(Ic.Back, onBack, size = 24, stroke = 2.1f) }, right = right)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = if (fab != null) 96.dp else 26.dp),
                content = content,
            )
        }
        if (fab != null) Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 20.dp)) { fab() }
    }
}

/* ---------- one tutor's thread (lecturer + admin) ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreadScreen(tutorId: String, myId: String, isLecturer: Boolean, previews: Boolean, vm: StaffViewModel, s: StaffState, onBack: () -> Unit) {
    LaunchedEffect(tutorId) { vm.loadThread(tutorId) }
    var composing by rememberSaveable { mutableStateOf(false) }
    val t = s.thread[tutorId] ?: Loadable.Loading
    val title = (t as? Loadable.Ready)?.data?.tutor?.name ?: "Thread"
    DetailPage(title, onBack, fab = if (t is Loadable.Ready) ({ Fab("New message", { composing = true }) }) else null) {
        Loaded(t, { vm.loadThread(tutorId) }) { d ->
            FlowRow(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                d.tutor.modules.forEach { ModuleChip(it, moduleName(it), small = true) }
            }
            if (d.messages.isEmpty()) {
                EmptyState("Nothing sent yet", "Send the first notice to ${d.tutor.name.substringBefore(' ')}.", Ic.Mail)
            } else {
                MessageTimeline(d.messages, myId, viewerIsTutor = false, previews = previews, onOpen = {})
            }
        }
    }
    if (composing) {
        ComposeSheet(title, s.busy, onDismiss = { composing = false }) { topic, body, close ->
            vm.send(tutorId, topic, body, isLecturer) { close() }
        }
    }
}

/* ---------- application detail (admin) ---------- */

private fun markColor(m: Int) = when { m >= 75 -> TL.Green; m >= 50 -> TL.Amber; else -> TL.Red }

@Composable
fun ApplicationScreen(id: String, vm: StaffViewModel, s: StaffState, onBack: () -> Unit) {
    LaunchedEffect(id) { vm.loadApp(id) }
    DetailPage("Application", onBack) {
        Loaded(s.app[id] ?: Loadable.Loading, { vm.loadApp(id) }) { a ->
            val templates = (s.templates as? Loadable.Ready)?.data.orEmpty()
            AppBody(a, templates, s.busy, vm, onBack)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.AppBody(a: AppDetail, templates: List<com.snokonoko.tutor_link.data.Template>, busy: Boolean, vm: StaffViewModel, onBack: () -> Unit) {
    val decided = a.status == "approved" || a.status == "rejected"
    val haptics = LocalHaptics.current
    var custom by rememberSaveable(a.id) { mutableStateOf(false) }
    var tplId by rememberSaveable(a.id) { mutableStateOf<Int?>(null) }
    var text by rememberSaveable(a.id) { mutableStateOf("") }
    val message = if (custom) text else templates.firstOrNull { it.id == tplId }?.body.orEmpty()
    val ready = message.isNotBlank()

    Rise(0) {
        TLCard(pad = true) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(initials(a.name), large = true)
                Column {
                    Text(a.name, style = TLType.rname.copy(fontSize = 16.sp))
                    Text(a.studentNumber, style = TLType.rmeta, modifier = Modifier.padding(top = 3.dp))
                }
            }
            FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                FlagBadge(a.flag)
                Badge(statusLabel(a.status), statusKind(a.status))
                if (a.backgroundChecked) Badge("Checked", BadgeKind.Green) else Badge("No check", BadgeKind.Plain)
            }
            Column(Modifier.padding(top = 14.dp)) {
                RowDivider()
                KV("Submitted", formatDate(a.submittedAt))
                RowDivider()
                KV("Programme", a.programme ?: "—")
            }
        }
    }

    Sect("Applied for")
    ModuleChips(a.appliedModules.map { ModuleDto(it, moduleName(it)) })

    Sect("Academic record · ${a.minMark.toInt()}% required")
    Rise(1) {
        TLCard(pad = true) {
            a.marks.forEachIndexed { i, m ->
                if (i > 0) RowDivider()
                val col = markColor(m.mark)
                Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.code, fontFamily = Mono, fontSize = 11.5.sp, fontWeight = if (m.appliedFor) FontWeight.Bold else FontWeight.Normal, color = if (m.appliedFor) TL.Text else TL.Muted)
                            if (m.appliedFor) {
                                Text(
                                    "APPLIED", fontFamily = Mono, fontSize = 8.5.sp, color = TL.GoldD,
                                    modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(3.dp)).background(TL.GoldBg).padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Text(m.name, style = TLType.rmeta.copy(fontSize = 11.sp), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                    }
                    Box(Modifier.width(62.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(TL.Bg).border(1.dp, TL.Line, RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(m.mark.coerceIn(0, 100) / 100f).fillMaxHeight().background(col))
                    }
                    Text("${m.mark}%", fontFamily = Mono, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = col, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
                    Box(Modifier.width(16.dp)) {
                        if (m.appliedFor) TLIcon(if (m.mark >= a.minMark) Ic.Check else Ic.X, 15, 2.2f, col)
                    }
                }
            }
        }
    }

    Sect("Background check")
    TLCard {
        ListRow("Verification", "Mark complete once the record is verified", trailing = {
            if (a.backgroundChecked) Badge("Done", BadgeKind.Green)
            else TLButton("Mark done", { vm.backgroundCheck(a.id) }, small = true, loading = busy, buzz = 12)
        })
    }

    if (decided) {
        Sect("Decision")
        TLCard(pad = true) {
            Row { Badge(statusLabel(a.status), statusKind(a.status)) }
            Text("Processed ${formatDate(a.processedAt)}", style = TLType.small, modifier = Modifier.padding(top = 10.dp))
            if (a.responseMessage.isNotBlank()) Text(a.responseMessage, style = TLType.small.copy(color = TL.Text, lineHeight = 21.sp), modifier = Modifier.padding(top = 10.dp))
        }
    } else {
        Sect("Response to the applicant")
        // segmented Template / Custom
        Row(Modifier.fillMaxWidth().clip(R4).border(1.dp, TL.Line, R4).background(TL.Bg)) {
            listOf(false to "Template", true to "Custom").forEach { (isCustom, label) ->
                val on = custom == isCustom
                Text(
                    label.uppercase(),
                    style = TLType.badge.copy(fontSize = 12.sp, color = if (on) TL.Gold else TL.Muted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).background(if (on) TL.Navy else Color.Transparent)
                        .clickable { haptics.buzz(Buzz.TAB); custom = isCustom }.padding(vertical = 12.dp),
                )
            }
        }
        if (!custom) {
            templates.forEach { t ->
                val on = tplId == t.id
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp).clip(R4)
                        .background(if (on) TL.GoldBg else TL.Card).border(1.5.dp, if (on) TL.Gold else TL.Line, R4)
                        .clickable { haptics.buzz(7); tplId = t.id }.padding(horizontal = 13.dp, vertical = 12.dp),
                ) {
                    Text(t.label, style = TLType.rname.copy(fontSize = 13.sp))
                    Text(t.body.take(96) + "…", style = TLType.rmeta.copy(fontSize = 11.5.sp, lineHeight = 17.sp), modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            Field("Message", text, { text = it.take(2000) }, placeholder = "Write a personal response…", minLines = 4, modifier = Modifier.padding(top = 10.dp))
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            TLButton("Approve", { vm.decide(a.id, true, message) { onBack() } }, Modifier.weight(1f), enabled = ready, loading = busy, icon = Ic.Check, buzz = Buzz.DECIDE)
            TLButton("Decline", { vm.decide(a.id, false, message) { onBack() } }, Modifier.weight(1f), style = BtnStyle.Danger, enabled = ready, icon = Ic.X, buzz = Buzz.DECIDE)
        }
        Text(
            "Approving creates the tutor account and sends them a welcome message.",
            style = TLType.small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )
    }
}

@Composable
private fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Text(k, style = TLType.small.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
        Text(v, style = TLType.rmeta.copy(fontSize = 12.sp, color = TL.Text), textAlign = TextAlign.End, modifier = Modifier.weight(1.5f))
    }
}

/* ---------- tutor detail (admin) ---------- */

@Composable
fun TutorDetailScreen(id: String, vm: StaffViewModel, s: StaffState, onBack: () -> Unit, onThread: (String) -> Unit) {
    LaunchedEffect(id) { vm.loadTutor(id) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val t = s.tutor[id] ?: Loadable.Loading
    val d = (t as? Loadable.Ready)?.data
    DetailPage(
        d?.tutor?.name ?: "Tutor", onBack,
        right = { IconBtn(Ic.Mail, { onThread(id) }) },
        fab = if (d != null) ({ Fab("Add session", { adding = true }) }) else null,
    ) {
        Loaded(t, { vm.loadTutor(id) }) { x ->
            Rise(0) {
                TLCard(pad = true) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(initials(x.tutor.name), large = true)
                        Column(Modifier.weight(1f)) {
                            Text(x.tutor.name, style = TLType.rname.copy(fontSize = 16.sp))
                            Text("${x.tutor.studentNumber ?: ""} · since ${formatDate(x.tutor.approvedAt)}", style = TLType.rmeta, modifier = Modifier.padding(top = 3.dp))
                        }
                        if (x.tutor.paid && x.pay.owed == 0.0) Badge("Paid", BadgeKind.Green) else Badge("Active", BadgeKind.Gold)
                    }
                    ModuleChips(x.modules, Modifier.padding(top = 14.dp))
                }
            }
            Rise(1) {
                MetricsRow(Modifier.padding(top = 14.dp)) {
                    Metric(x.pay.sessions.toString(), "Sessions", MetricKind.Navy, Modifier.weight(1f))
                    Metric(x.pay.pending.toString(), "To confirm", MetricKind.Amber, Modifier.weight(1f))
                    Metric(PayMath.rand(x.pay.owed), "Owed", MetricKind.Gold, Modifier.weight(1f), small = true)
                }
            }
            Sect("Sessions")
            Rise(2) {
                TLCard {
                    if (x.sessions.isEmpty()) ListRow("No sessions logged yet.")
                    x.sessions.forEachIndexed { i, ss ->
                        if (i > 0) RowDivider()
                        ListRow(
                            formatDate(ss.date), "${moduleName(ss.moduleCode)} · ${ss.studentCount} students · by ${ss.loggedBy}",
                            leading = { Avatar(ss.moduleCode.take(4), AvatarKind.Code) },
                            trailing = {
                                if (ss.confirmed) Badge(if (ss.paid) "Paid" else "Confirmed", BadgeKind.Green)
                                else TLButton("Confirm", { vm.confirm(ss.id, id) }, small = true, enabled = !s.busy, buzz = Buzz.CONFIRM)
                            },
                        )
                    }
                }
            }
            Sect("Attendance registers")
            TLCard {
                if (x.registers.isEmpty()) ListRow("Nothing uploaded yet.")
                x.registers.forEachIndexed { i, r ->
                    if (i > 0) RowDivider()
                    ListRow(r.fileName, "${r.moduleCode} · ${formatDate(r.uploadedAt)}", leading = {
                        Box(Modifier.clip(R4).background(TL.Bg).border(1.dp, TL.Line, R4).padding(11.dp)) { TLIcon(Ic.Doc, 18) }
                    })
                }
            }
            Sect("Payment")
            TLCard(edge = TL.Gold, pad = true) {
                Text(PayMath.rand(x.pay.owed), style = TLType.money)
                Text(
                    "${x.pay.confirmedUnpaid} confirmed unpaid session${if (x.pay.confirmedUnpaid == 1) "" else "s"} at ${PayMath.rand(x.pay.rate)}",
                    style = TLType.small, modifier = Modifier.padding(top = 6.dp),
                )
                if (x.tutor.paid && x.pay.confirmedUnpaid == 0) {
                    Note("All confirmed sessions marked paid.", kind = NoteKind.Green, icon = Ic.Check, modifier = Modifier.padding(top = 14.dp))
                } else {
                    TLButton(
                        "Mark as paid", { vm.markPaid(id, x.tutor.name) }, Modifier.padding(top = 14.dp),
                        enabled = x.pay.confirmedUnpaid > 0, loading = s.busy, buzz = Buzz.DECIDE,
                    )
                }
            }
        }
    }
    if (adding && d != null) {
        LogSessionSheet(d.modules, s.busy, onDismiss = { adding = false }, admin = true) { date, mod, n, close ->
            vm.addSession(id, date, mod, n, close)
        }
    }
}


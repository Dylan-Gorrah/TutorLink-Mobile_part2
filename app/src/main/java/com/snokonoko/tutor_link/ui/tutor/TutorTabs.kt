package com.snokonoko.tutor_link.ui.tutor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.snokonoko.tutor_link.data.Me
import com.snokonoko.tutor_link.data.ModuleDto
import com.snokonoko.tutor_link.data.PayResponse
import com.snokonoko.tutor_link.data.ScheduleResponse
import com.snokonoko.tutor_link.data.SessionDto
import com.snokonoko.tutor_link.data.moduleName
import com.snokonoko.tutor_link.domain.PayMath
import com.snokonoko.tutor_link.ui.components.Avatar
import com.snokonoko.tutor_link.ui.components.AvatarKind
import com.snokonoko.tutor_link.ui.components.Badge
import com.snokonoko.tutor_link.ui.components.BadgeKind
import com.snokonoko.tutor_link.ui.components.BtnStyle
import com.snokonoko.tutor_link.ui.components.Buzz
import com.snokonoko.tutor_link.ui.components.Display
import com.snokonoko.tutor_link.ui.components.EmptyState
import com.snokonoko.tutor_link.ui.components.ErrorBlock
import com.snokonoko.tutor_link.ui.components.Eyebrow
import com.snokonoko.tutor_link.ui.components.Fab
import com.snokonoko.tutor_link.ui.components.Field
import com.snokonoko.tutor_link.ui.components.Ic
import com.snokonoko.tutor_link.ui.components.ListRow
import com.snokonoko.tutor_link.ui.components.LoadingBlock
import com.snokonoko.tutor_link.ui.components.LocalHaptics
import com.snokonoko.tutor_link.ui.components.MessageTimeline
import com.snokonoko.tutor_link.ui.components.Metric
import com.snokonoko.tutor_link.ui.components.MetricKind
import com.snokonoko.tutor_link.ui.components.MetricsRow
import com.snokonoko.tutor_link.ui.components.ModuleChip
import com.snokonoko.tutor_link.ui.components.ModuleChips
import com.snokonoko.tutor_link.ui.components.Note
import com.snokonoko.tutor_link.ui.components.NoteKind
import com.snokonoko.tutor_link.ui.components.Rise
import com.snokonoko.tutor_link.ui.components.RowDivider
import com.snokonoko.tutor_link.ui.components.Sect
import com.snokonoko.tutor_link.ui.components.TLButton
import com.snokonoko.tutor_link.ui.components.TLCard
import com.snokonoko.tutor_link.ui.components.TLIcon
import com.snokonoko.tutor_link.ui.components.leftEdge
import com.snokonoko.tutor_link.ui.screens.TLSheet
import com.snokonoko.tutor_link.ui.screens.formatDate
import com.snokonoko.tutor_link.ui.theme.R4
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TLType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Draws one tutor tab. Each tab pulls to refresh and has loading / empty / error states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorTab(tab: String, me: Me, vm: TutorViewModel, state: TutorState, previews: Boolean, goTab: (String) -> Unit) {
    var sheet by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = { vm.refresh(tab) }, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = if (tab == "sessions") 96.dp else 26.dp),
            ) {
                when (tab) {
                    "home" -> Loaded(state.schedule, vm::loadSchedule) { HomeTab(me, it, state.busy, vm::mark, goTab) }
                    "sessions" -> Loaded(state.sessions, vm::loadSessions) { SessionsTab(it, state, vm) { sheet = "upload" } }
                    "inbox" -> Loaded(state.messages, vm::loadMessages) { list ->
                        if (list.isEmpty()) {
                            EmptyState("No messages", "Your lecturer and the programme admin will reach you here.")
                        } else {
                            Eyebrow("Notices · ${list.size}")
                            MessageTimeline(list, me.account.id, viewerIsTutor = true, previews = previews, onOpen = vm::open)
                        }
                    }
                    "pay" -> Loaded(state.pay, vm::loadPay) { PayTab(me, it, state.sessions, vm) }
                }
            }
        }
        if (tab == "sessions") {
            Fab("Log session", { sheet = "log" }, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp))
        }
    }
    when (sheet) {
        "log" -> LogSessionSheet(me.modules, state.busy, onDismiss = { sheet = null }) { date, mod, n, close ->
            vm.logSession(date, mod, n, close)
        }
        "upload" -> UploadSheet(me, state.busy, vm, onDismiss = { sheet = null })
    }
}

@Composable
private fun <T> Loaded(value: Loadable<T>, retry: () -> Unit, content: @Composable (T) -> Unit) {
    when (value) {
        Loadable.Loading -> LoadingBlock()
        is Loadable.Failed -> ErrorBlock(value.message, retry)
        is Loadable.Ready -> content(value.data)
    }
}

/* ---------- Schedule ---------- */

@Composable
private fun ColumnScope.HomeTab(me: Me, s: ScheduleResponse, busy: Boolean, onMark: (String) -> Unit, goTab: (String) -> Unit) {
    Eyebrow("Welcome back")
    Display(me.account.name.substringBefore(' '), small = true)
    ModuleChips(s.modules, Modifier.padding(top = 14.dp))

    if (s.unread > 0) {
        Note(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${s.unread} unread message${if (s.unread > 1) "s" else ""}") }
                append(" from your lecturer or admin")
            },
            kind = NoteKind.Gold,
            icon = Ic.Mail,
            modifier = Modifier.padding(top = 18.dp),
            onClick = { goTab("inbox") },
        )
    }

    Sect("Upcoming")
    Rise(0) {
        TLCard {
            if (s.upcoming.isEmpty()) ListRow("Nothing scheduled this week.")
            s.upcoming.forEachIndexed { i, u ->
                if (i > 0) RowDivider()
                ListRow(
                    "${u.day} · ${u.time}",
                    "${u.venue} · ${u.studentCount} students · ${moduleName(u.moduleCode)}",
                    leading = { Avatar(u.moduleCode.take(4), if (u.live) AvatarKind.Gold else AvatarKind.Code) },
                    trailing = {
                        when {
                            u.live && !u.marked -> TLButton("Mark", { onMark(u.id) }, style = BtnStyle.Outline, small = true, enabled = !busy, buzz = Buzz.CONFIRM)
                            u.marked -> Badge("Logged", BadgeKind.Green)
                            else -> Text(u.day, style = TLType.stamp)
                        }
                    },
                )
            }
        }
    }

    Sect("This month")
    Rise(1) {
        MetricsRow {
            Metric(s.month.sessions.toString(), "Sessions", MetricKind.Navy, Modifier.weight(1f))
            Metric(s.month.confirmed.toString(), "Confirmed", MetricKind.Green, Modifier.weight(1f))
            Metric(PayMath.rand(s.month.earned), "Earned", MetricKind.Gold, Modifier.weight(1f), small = true)
        }
    }

    if (s.pending.isNotEmpty()) {
        Sect("Awaiting admin confirmation")
        Rise(2) {
            TLCard {
                s.pending.forEachIndexed { i, p ->
                    if (i > 0) RowDivider()
                    SessionRow(p, showLoggedBy = false)
                }
            }
        }
    }
}

@Composable
private fun SessionRow(s: SessionDto, showLoggedBy: Boolean) {
    ListRow(
        moduleName(s.moduleCode),
        "${formatDate(s.date)} · ${s.studentCount} students" + if (showLoggedBy) " · logged by ${s.loggedBy}" else "",
        leading = { Avatar(s.moduleCode.take(4), AvatarKind.Code) },
        trailing = {
            when {
                s.paid -> Badge("Paid", BadgeKind.Green)
                s.confirmed -> Badge("Confirmed", BadgeKind.Green)
                else -> Badge("Pending", BadgeKind.Amber)
            }
        },
    )
}

/* ---------- Sessions ---------- */

@Composable
private fun ColumnScope.SessionsTab(sessions: List<SessionDto>, state: TutorState, vm: TutorViewModel, onUpload: () -> Unit) {
    Sect("Logged sessions · ${sessions.size}", first = true)
    Rise(0) {
        TLCard {
            if (sessions.isEmpty()) ListRow("No sessions yet — tap Log session.")
            sessions.forEachIndexed { i, s ->
                if (i > 0) RowDivider()
                SessionRow(s, showLoggedBy = true)
            }
        }
    }
    Sect("Attendance registers", action = "Upload" to onUpload)
    Rise(1) {
        TLCard {
            when (val r = state.registers) {
                Loadable.Loading -> ListRow("Loading registers…")
                is Loadable.Failed -> ListRow("Couldn't load registers", r.message, onClick = { vm.loadRegisters() })
                is Loadable.Ready -> {
                    if (r.data.isEmpty()) ListRow("No registers uploaded yet.")
                    r.data.forEachIndexed { i, d ->
                        if (i > 0) RowDivider()
                        ListRow(
                            d.fileName,
                            "${d.moduleCode} · uploaded ${formatDate(d.uploadedAt)}",
                            leading = {
                                Box(Modifier.clip(R4).background(TL.Bg).border(1.dp, TL.Line, R4).padding(11.dp)) { TLIcon(Ic.Doc, 18) }
                            },
                        )
                    }
                }
            }
        }
    }
    Note("A session only counts towards pay once admin confirms it against the signed register.", modifier = Modifier.padding(top = 12.dp))
}

/* ---------- Pay ---------- */

@Composable
private fun ColumnScope.PayTab(me: Me, p: PayResponse, sessions: Loadable<List<SessionDto>>, vm: TutorViewModel) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    Rise(0) {
        TLCard(edge = TL.Gold, pad = true) {
            Eyebrow("Approved earnings", Modifier.padding(bottom = 0.dp))
            Text(PayMath.rand(p.approved), style = TLType.money, modifier = Modifier.padding(top = 0.dp))
            Text(
                "${p.confirmedCount} confirmed session${if (p.confirmedCount == 1) "" else "s"} at ${PayMath.rand(p.rate)} each",
                style = TLType.small,
                modifier = Modifier.padding(top = 8.dp),
            )
            Column(Modifier.padding(top = 14.dp)) {
                RowDivider()
                PayRow("Rate per session", PayMath.rand(p.rate), TL.Text)
                RowDivider()
                PayRow("Awaiting confirmation", PayMath.rand(p.pendingAmount), TL.Amber)
                RowDivider()
                PayRow("Projected total", PayMath.rand(p.projected), TL.GoldD)
            }
        }
    }
    Sect("Breakdown")
    Rise(1) {
        TLCard {
            if (p.lines.isEmpty()) ListRow("Nothing confirmed yet.")
            p.lines.forEachIndexed { i, l ->
                if (i > 0) RowDivider()
                ListRow(
                    moduleName(l.moduleCode),
                    "${formatDate(l.date)} · ${l.studentCount} students",
                    leading = { Avatar(l.moduleCode.take(4), AvatarKind.Code) },
                    trailing = { Text(PayMath.rand(l.amount), style = TLType.rname.copy(color = TL.Green, fontSize = 13.sp)) },
                )
            }
        }
    }
    TLButton(
        "Export statement (CSV)",
        {
            val list = (sessions as? Loadable.Ready)?.data
            if (list == null) {
                vm.toast("Still loading your sessions. Try again in a moment.", error = true)
            } else {
                scope.launch {
                    val ok = shareCsv(context, me.account.name, PayMath.csv(list, p.rate, ::moduleName))
                    if (ok) haptics.buzz(10) else vm.toast("Couldn't create the statement.", error = true)
                }
            }
        },
        style = BtnStyle.Outline,
        icon = Ic.Down,
        enabled = p.lines.isNotEmpty(),
        modifier = Modifier.padding(top = 16.dp),
    )
    if (p.paid) {
        Note("Marked as paid by admin.", kind = NoteKind.Green, icon = Ic.Check, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun PayRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Text(label, style = TLType.small.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
        Text(value, style = TLType.rname.copy(fontSize = 13.sp, color = color))
    }
}

/** Writes the CSV to the cache and opens Android's share sheet. */
private suspend fun shareCsv(context: Context, name: String, csv: String): Boolean = try {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "statements").apply { mkdirs() }
        File(dir, name.replace(Regex("\\s+"), "_") + "_earnings.csv").apply { writeText(csv) }
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "TutorLink earnings statement")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Export statement").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: Exception) {
    false
}

/* ---------- Log session sheet ---------- */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LogSessionSheet(
    modules: List<ModuleDto>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, () -> Unit) -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var module by remember { mutableStateOf(modules.firstOrNull()?.code ?: "") }
    var students by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }

    TLSheet("Tutor", "Log a session", "It stays pending until admin checks it against your signed register.", onDismiss) { close ->
        Column {
            Text("DATE", style = TLType.label, modifier = Modifier.padding(start = 1.dp, bottom = 6.dp))
            Row(
                Modifier.fillMaxWidth().clip(R4).background(TL.Card).border(1.5.dp, TL.Line, R4)
                    .clickable { picking = true }.padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatDate(date), style = TLType.input, modifier = Modifier.weight(1f))
                TLIcon(Ic.Cal, 18, tint = TL.Muted)
            }
        }
        Column {
            Text("MODULE", style = TLType.label, modifier = Modifier.padding(start = 1.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                modules.forEach { m -> ModuleChip(m.code, m.name, on = m.code == module) { module = m.code } }
            }
        }
        Field("Students attending", students, { students = it.filter(Char::isDigit).take(3) }, placeholder = "e.g. 8", keyboard = KeyboardType.Number)
        TLButton("Log session", { onSave(date, module, students, close) }, loading = busy, buzz = Buzz.CONFIRM)
    }

    if (picking) {
        val todayUtc = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton({
                    pickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    picking = false
                }) { Text("OK", color = TL.Navy) }
            },
            dismissButton = { TextButton({ picking = false }) { Text("Cancel", color = TL.Muted) } },
        ) {
            DatePicker(
                pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = TL.Navy,
                    selectedDayContentColor = TL.Gold,
                    todayDateBorderColor = TL.Gold,
                    todayContentColor = TL.Navy,
                ),
            )
        }
    }
}

/* ---------- Upload register sheet ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UploadSheet(me: Me, busy: Boolean, vm: TutorViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var module by remember { mutableStateOf(me.modules.firstOrNull()?.code ?: "") }
    var fileName by remember { mutableStateOf<String?>(null) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var mime by remember { mutableStateOf("application/pdf") }
    var reading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        reading = true
        scope.launch {
            val picked = withContext(Dispatchers.IO) { readFile(context, uri) }
            reading = false
            when {
                picked == null -> vm.toast("Couldn't read that file. Try another one.", error = true)
                picked.second.size > 10 * 1024 * 1024 -> vm.toast("That file is over 10 MB. Try a smaller photo or PDF.", error = true)
                picked.third !in ALLOWED_TYPES -> vm.toast("Use a PDF or a photo (JPG, PNG, HEIC).", error = true)
                else -> { fileName = picked.first; bytes = picked.second; mime = picked.third }
            }
        }
    }

    TLSheet("Attendance", "Upload register", "Admin confirms your sessions against this document.", onDismiss) { close ->
        Column {
            Text("MODULE", style = TLType.label, modifier = Modifier.padding(start = 1.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                me.modules.forEach { m -> ModuleChip(m.code, m.name, on = m.code == module) { module = m.code } }
            }
        }
        val done = bytes != null
        Column(
            Modifier.fillMaxWidth().clip(R4)
                .background(if (done) TL.GreenBg else TL.Bg)
                .border(1.5.dp, if (done) TL.GreenBd else TL.Line, R4)
                .padding(horizontal = 16.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TLIcon(if (done) Ic.Check else if (reading) Ic.Clock else Ic.Upload, 30, if (done) 2f else 1.7f, if (done) TL.Green else TL.Muted)
            Text(
                when { done -> "Ready to upload"; reading -> "Reading document…"; else -> "Choose the signed register" },
                style = TLType.rname.copy(fontSize = 13.5.sp),
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                if (done) fileName ?: "" else "PDF or a photo of the sheet",
                style = TLType.rmeta.copy(fontSize = 11.5.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (!reading) {
                Spacer(Modifier.height(12.dp))
                TLButton(if (done) "Choose another" else "Browse", { picker.launch(ALLOWED_TYPES.toTypedArray()) }, style = BtnStyle.Outline, small = true)
            }
        }
        TLButton(
            "Upload register",
            { val b = bytes; val n = fileName; if (b != null && n != null) vm.uploadRegister(me.account.id, module, n, mime, b, close) },
            enabled = bytes != null && module.isNotBlank(),
            loading = busy,
            buzz = Buzz.CONFIRM,
        )
    }
}

private val ALLOWED_TYPES = listOf("application/pdf", "image/jpeg", "image/png", "image/heic", "image/webp")

private fun readFile(context: Context, uri: Uri): Triple<String, ByteArray, String>? = try {
    var name = "register"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0)?.let { name = it }
    }
    val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
    if (data == null) null else Triple(name, data, type)
} catch (_: Exception) {
    null
}


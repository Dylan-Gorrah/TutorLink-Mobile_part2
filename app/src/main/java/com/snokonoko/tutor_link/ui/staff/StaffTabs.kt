package com.snokonoko.tutor_link.ui.staff

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.data.AdminTutor
import com.snokonoko.tutor_link.data.Me
import com.snokonoko.tutor_link.data.MessageDto
import com.snokonoko.tutor_link.domain.MessageLogic
import com.snokonoko.tutor_link.domain.PayMath
import com.snokonoko.tutor_link.ui.components.Avatar
import com.snokonoko.tutor_link.ui.components.AvatarKind
import com.snokonoko.tutor_link.ui.components.Badge
import com.snokonoko.tutor_link.ui.components.BadgeKind
import com.snokonoko.tutor_link.ui.components.BtnStyle
import com.snokonoko.tutor_link.ui.components.Buzz
import com.snokonoko.tutor_link.ui.components.EmptyState
import com.snokonoko.tutor_link.ui.components.ErrorBlock
import com.snokonoko.tutor_link.ui.components.Eyebrow
import com.snokonoko.tutor_link.ui.components.Fab
import com.snokonoko.tutor_link.ui.components.Field
import com.snokonoko.tutor_link.ui.components.FilterChip
import com.snokonoko.tutor_link.ui.components.Ic
import com.snokonoko.tutor_link.ui.components.ListRow
import com.snokonoko.tutor_link.ui.components.LoadingBlock
import com.snokonoko.tutor_link.ui.components.Metric
import com.snokonoko.tutor_link.ui.components.MetricKind
import com.snokonoko.tutor_link.ui.components.MetricsRow
import com.snokonoko.tutor_link.ui.components.ModuleChips
import com.snokonoko.tutor_link.ui.components.Note
import com.snokonoko.tutor_link.ui.components.NoteKind
import com.snokonoko.tutor_link.ui.components.ProfileCard
import com.snokonoko.tutor_link.ui.components.Rise
import com.snokonoko.tutor_link.ui.components.RowDivider
import com.snokonoko.tutor_link.ui.components.Sect
import com.snokonoko.tutor_link.ui.components.TLButton
import com.snokonoko.tutor_link.ui.components.TLCard
import com.snokonoko.tutor_link.ui.components.TLIcon
import com.snokonoko.tutor_link.ui.components.UnreadDot
import com.snokonoko.tutor_link.ui.components.initials
import com.snokonoko.tutor_link.ui.screens.TLSheet
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TLType
import com.snokonoko.tutor_link.ui.tutor.Loadable

/** What a staff tab can ask the app to do. */
class StaffNav(
    val openThread: (String) -> Unit,
    val openApp: (String) -> Unit,
    val openTutor: (String) -> Unit,
    val editModules: () -> Unit,
    val openSettings: () -> Unit,
    val signOut: () -> Unit,
)

@Composable
fun <T> Loaded(value: Loadable<T>, retry: () -> Unit, content: @Composable (T) -> Unit) {
    when (value) {
        Loadable.Loading -> LoadingBlock()
        is Loadable.Failed -> ErrorBlock(value.message, retry)
        is Loadable.Ready -> content(value.data)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabBody(refreshing: Boolean, onRefresh: () -> Unit, fabSpace: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = if (fabSpace) 96.dp else 26.dp),
            content = content,
        )
    }
}

/* ================= LECTURER ================= */

@Composable
fun LecturerTab(tab: String, me: Me, vm: StaffViewModel, s: StaffState, firstRun: Boolean, nav: StaffNav, goTab: (String) -> Unit) {
    TabBody(s.refreshing, { vm.refresh("lecturer") }) {
        when (tab) {
            "tutors" -> Loaded(s.lecturer, { vm.loadLecturer() }) { d ->
                val chooseBtn: @Composable () -> Unit = { TLButton("Choose modules", nav.editModules, style = BtnStyle.Outline, small = true) }
                when {
                    d.modules.isEmpty() -> EmptyState("No modules yet", "Pick the modules you teach and the tutors on them appear here.", Ic.Board, chooseBtn)
                    d.tutors.isEmpty() -> {
                        Eyebrow("Modules you teach")
                        ModuleChips(d.modules)
                        EmptyState(
                            "No tutors yet",
                            "Nobody has been approved to tutor ${if (d.modules.size > 1) "these modules" else d.modules[0].code} so far. As soon as admin approves one, they appear here and you can message them.",
                            Ic.People, chooseBtn,
                        )
                    }
                    else -> {
                        Eyebrow("Modules you teach")
                        ModuleChips(d.modules)
                        Rise(0) {
                            MetricsRow(Modifier.padding(top = 16.dp)) {
                                Metric(d.counts.tutors.toString(), "Tutors", MetricKind.Navy, Modifier.weight(1f))
                                Metric(d.counts.modules.toString(), "Modules", MetricKind.Gold, Modifier.weight(1f))
                                Metric(d.counts.sessions.toString(), "Sessions run", MetricKind.Green, Modifier.weight(1f))
                            }
                        }
                        if (firstRun && !d.hasSentMessage) {
                            Note(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Your account is ready.") }
                                    append(" These are the tutors on your modules — tap a name to read the thread and send your first notice.")
                                },
                                kind = NoteKind.Gold, icon = Ic.Mail, modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                        Sect("Tutors · ${d.tutors.size}")
                        Rise(1) {
                            TLCard {
                                d.tutors.forEachIndexed { i, t ->
                                    if (i > 0) RowDivider()
                                    ListRow(
                                        t.name,
                                        t.lastMessage?.topic ?: t.modules.joinToString(" · "),
                                        onClick = { nav.openThread(t.id) },
                                        leading = { Avatar(initials(t.name)) },
                                        trailing = {
                                            t.lastMessage?.let { Text(MessageLogic.ago(it.sentAt), style = TLType.stamp) }
                                            if (t.unread) UnreadDot()
                                        },
                                    )
                                }
                            }
                        }
                        Note("You only see tutors on the modules you teach. Tap a name to open the thread.", modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            "messages" -> Loaded(s.sent, { vm.loadLecturer() }) { list ->
                val hasTutors = ((s.lecturer as? Loadable.Ready)?.data?.tutors?.isNotEmpty()) == true
                if (list.isEmpty()) {
                    EmptyState(
                        "Nothing sent yet",
                        if (hasTutors) "Everything you send to your tutors collects here. Open one from your dashboard to write the first notice."
                        else "Once there are tutors on your modules, everything you send them collects here.",
                        Ic.Mail,
                        if (hasTutors) ({ TLButton("Go to my tutors", { goTab("tutors") }, style = BtnStyle.Outline, small = true) }) else null,
                    )
                } else {
                    Eyebrow("Sent by you")
                    Rise(0) {
                        TLCard {
                            list.forEachIndexed { i, m ->
                                if (i > 0) RowDivider()
                                ListRow(
                                    m.topic,
                                    "To ${m.tutorName ?: ""}${if (m.readByTutor) "" else " · delivered"}",
                                    onClick = { nav.openThread(m.tutorId) },
                                    leading = { Avatar(initials(m.tutorName ?: "?"), AvatarKind.Gold) },
                                    trailing = { Text(MessageLogic.ago(m.sentAt), style = TLType.stamp) },
                                )
                            }
                        }
                    }
                    Note("Notices from admin and other lecturers sit in each tutor's thread — tap one to read the full record.", modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

/** Tutors per module, for the lecturer's profile. */
fun tutorCounts(s: StaffState): Map<String, Int> {
    val d = (s.lecturer as? Loadable.Ready)?.data ?: return emptyMap()
    return d.modules.associate { m -> m.code to d.tutors.count { m.code in it.modules } }
}

/* ================= ADMIN ================= */

private val FILTERS = listOf("all" to "All", "pending" to "Pending", "under_review" to "Under Review", "approved" to "Approved", "rejected" to "Rejected")

fun statusLabel(s: String) = when (s) { "approved" -> "Approved"; "rejected" -> "Rejected"; "under_review" -> "Under Review"; else -> "Pending" }
fun statusKind(s: String) = when (s) { "approved" -> BadgeKind.Green; "rejected" -> BadgeKind.Red; "under_review" -> BadgeKind.Amber; else -> BadgeKind.Slate }

@Composable
fun FlagBadge(flag: String?) = when (flag) {
    "pass" -> Badge("Eligible", BadgeKind.Green)
    "fail" -> Badge("Ineligible", BadgeKind.Red)
    else -> Badge("Pending", BadgeKind.Plain)
}

@Composable
fun AdminTab(tab: String, me: Me, vm: StaffViewModel, s: StaffState, nav: StaffNav, goTab: (String) -> Unit) {
    var sheet by remember { mutableStateOf<String?>(null) }
    var composeFor by remember { mutableStateOf<AdminTutor?>(null) }
    Box(Modifier.fillMaxSize()) {
        TabBody(s.refreshing, { vm.refresh("admin") }, fabSpace = tab == "messages") {
            when (tab) {
                "home" -> Loaded(s.overview, { vm.loadAdmin() }) { o -> Overview(o, s, nav, goTab) }
                "apps" -> Loaded(s.applications, { vm.loadAdmin() }) { apps -> Applications(apps, nav) }
                "tutors" -> Loaded(s.tutors, { vm.loadAdmin() }) { tutors ->
                    Eyebrow("Active tutors · ${tutors.size}")
                    Rise(0) {
                        TLCard {
                            tutors.forEachIndexed { i, t ->
                                if (i > 0) RowDivider()
                                ListRow(
                                    t.name, t.email, onClick = { nav.openTutor(t.id) },
                                    leading = { Avatar(initials(t.name)) },
                                    trailing = { if (t.paid && t.owed == 0.0) Badge("Paid", BadgeKind.Green) else Badge(PayMath.rand(t.owed), BadgeKind.Gold) },
                                )
                            }
                        }
                    }
                }
                "messages" -> Loaded(s.threads, { vm.loadAdmin() }) { list -> AdminMessages(list, me.account.id, nav) }
                "profile" -> AdminSettings(me, s, vm, nav) { sheet = it }
            }
        }
        if (tab == "messages") {
            Fab("New message", { sheet = "pick" }, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp))
        }
    }

    when (sheet) {
        "pick" -> TLSheet("Message", "Choose a tutor", null, { sheet = null }) { _ ->
            val tutors = (s.tutors as? Loadable.Ready)?.data.orEmpty()
            TLCard {
                if (tutors.isEmpty()) ListRow("No tutors yet")
                tutors.forEachIndexed { i, t ->
                    if (i > 0) RowDivider()
                    ListRow(t.name, t.modules.joinToString(" · "), onClick = { composeFor = t; sheet = "compose" }, leading = { Avatar(initials(t.name)) })
                }
            }
        }
        "compose" -> composeFor?.let { t ->
            ComposeSheet(t.name, s.busy, onDismiss = { sheet = null }) { topic, body, close ->
                vm.send(t.id, topic, body, isLecturer = false) { close(); nav.openThread(t.id) }
            }
        }
        "lecturer" -> AddLecturerSheet(s.busy, onDismiss = { sheet = null }) { n, e, mods, close -> vm.addLecturer(n, e, mods, close) }
        "rate" -> NumberSheet(
            "Rate per session", "Applies to every confirmed session across the programme.", "Rate (ZAR)",
            ((s.programme as? Loadable.Ready)?.data?.ratePerSession)?.let { PayMath.rand(it).removePrefix("R").replace(" ", "") } ?: "", "Save rate", s.busy,
            onDismiss = { sheet = null },
        ) { v, close -> vm.saveRate(v, close) }
        "minmark" -> NumberSheet(
            "Minimum mark", "Every module an applicant applies for needs at least this mark to be flagged eligible.", "Minimum mark (%)",
            ((s.programme as? Loadable.Ready)?.data?.minMark)?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "", "Save minimum mark", s.busy,
            onDismiss = { sheet = null },
        ) { v, close -> vm.saveMinMark(v, close) }
    }
}

@Composable
private fun ColumnScope.Overview(o: com.snokonoko.tutor_link.data.AdminOverview, s: StaffState, nav: StaffNav, goTab: (String) -> Unit) {
    Eyebrow("Programme")
    Rise(0) {
        MetricsRow {
            Metric(o.openApplications.toString(), "Open applications", MetricKind.Amber, Modifier.weight(1f))
            Metric(o.activeTutors.toString(), "Active tutors", MetricKind.Navy, Modifier.weight(1f))
            Metric(PayMath.rand(o.outstanding), "Outstanding", MetricKind.Gold, Modifier.weight(1f), small = true)
        }
    }
    if (o.awaitingConfirmation > 0) {
        Note(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${o.awaitingConfirmation} session${if (o.awaitingConfirmation > 1) "s" else ""}") }
                append(" logged by tutors are waiting for your confirmation.")
            },
            kind = NoteKind.Amber, icon = Ic.Clock, modifier = Modifier.padding(top = 14.dp), onClick = { goTab("tutors") },
        )
    }
    Sect("Recent applications", action = "View all" to { goTab("apps") })
    Rise(1) {
        TLCard {
            val apps = (s.applications as? Loadable.Ready)?.data.orEmpty().take(4)
            if (apps.isEmpty()) ListRow("No applications yet.")
            apps.forEachIndexed { i, a ->
                if (i > 0) RowDivider()
                ListRow(
                    a.name, "${a.studentNumber} · ${a.appliedModules.size} module${if (a.appliedModules.size == 1) "" else "s"}",
                    onClick = { nav.openApp(a.id) }, leading = { Avatar(initials(a.name)) },
                    trailing = { Badge(statusLabel(a.status), statusKind(a.status)) },
                )
            }
        }
    }
    Sect("Tutors", action = "View all" to { goTab("tutors") })
    Rise(2) {
        TLCard {
            val tutors = (s.tutors as? Loadable.Ready)?.data.orEmpty().sortedByDescending { it.owed }.take(5)
            tutors.forEachIndexed { i, t ->
                if (i > 0) RowDivider()
                ListRow(
                    t.name, t.modules.joinToString(" · "), onClick = { nav.openTutor(t.id) }, leading = { Avatar(initials(t.name)) },
                    trailing = { Text(PayMath.rand(t.owed), style = TLType.rname.copy(color = TL.GoldD, fontSize = 13.sp)) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Applications(apps: List<com.snokonoko.tutor_link.data.AppSummary>, nav: StaffNav) {
    var filter by rememberSaveable { mutableStateOf("all") }
    val rows = if (filter == "all") apps else apps.filter { it.status == filter }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        FILTERS.forEach { (id, label) -> FilterChip(label, filter == id) { filter = id } }
    }
    Sect("${rows.size} application${if (rows.size == 1) "" else "s"}", modifier = Modifier.padding(top = 4.dp))
    Rise(0) {
        TLCard {
            if (rows.isEmpty()) ListRow("Nothing in this filter.")
            rows.forEachIndexed { i, a ->
                if (i > 0) RowDivider()
                ListRow(
                    a.name, a.appliedModules.joinToString(" · "), onClick = { nav.openApp(a.id) },
                    leading = { Avatar(initials(a.name)) }, trailing = { FlagBadge(a.flag) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.AdminMessages(list: List<MessageDto>, myId: String, nav: StaffNav) {
    if (list.isEmpty()) {
        EmptyState("No messages yet", "Tap New message to reach a tutor.", Ic.Mail)
        return
    }
    Eyebrow("All threads")
    Rise(0) {
        TLCard {
            list.forEachIndexed { i, m ->
                if (i > 0) RowDivider()
                val mine = m.senderId == myId
                ListRow(
                    m.topic, "${m.tutorName ?: ""} · from ${if (mine) "you" else m.senderName}",
                    onClick = { nav.openThread(m.tutorId) },
                    leading = { Avatar(initials(m.tutorName ?: "?"), if (mine) AvatarKind.Gold else AvatarKind.Navy) },
                    trailing = {
                        Text(MessageLogic.ago(m.sentAt), style = TLType.stamp)
                        if (!m.readByTutor) UnreadDot()
                    },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.AdminSettings(me: Me, s: StaffState, vm: StaffViewModel, nav: StaffNav, openSheet: (String) -> Unit) {
    Rise(0) { ProfileCard(me.account.name, "Administrator · Rosebank College") }
    Sect("Programme configuration")
    Loaded(s.programme, { vm.loadAdmin() }) { p ->
        Rise(1) {
            TLCard {
                ListRow("Rate per session", "Paid on each confirmed session", onClick = { openSheet("rate") }, trailing = {
                    Text(PayMath.rand(p.ratePerSession), style = TLType.rname.copy(color = TL.GoldD, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                })
                RowDivider()
                ListRow("Minimum mark", "Required in every module applied for", onClick = { openSheet("minmark") }, trailing = {
                    Text("${p.minMark.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }}%", style = TLType.rname.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                })
                RowDivider()
                ListRow("Add a lecturer", "They sign in with Google the first time", onClick = { openSheet("lecturer") }, leading = { TLIcon(Ic.Plus, 20) })
                RowDivider()
                val sent = (s.threads as? Loadable.Ready)?.data?.size
                ListRow("Messages sent", "Across all tutors", trailing = { Text(sent?.toString() ?: "—", style = TLType.rname.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)) })
            }
        }
    }
    Sect("Response templates")
    Loaded(s.templates, { vm.loadAdmin() }) { list ->
        Rise(2) {
            TLCard {
                list.forEachIndexed { i, t ->
                    if (i > 0) RowDivider()
                    ListRow(t.label, t.body.take(54) + "…")
                }
            }
        }
    }
    Sect("Personal")
    Rise(3) {
        TLCard {
            ListRow("Settings", "Haptics, animations, message previews, password", onClick = nav.openSettings, leading = { TLIcon(Ic.Sliders, 20) })
        }
    }
    TLButton("Sign out", nav.signOut, style = BtnStyle.Outline, modifier = Modifier.padding(top = 18.dp), buzz = 10)
}

/* ---------- sheets shared by lecturer and admin ---------- */

@Composable
fun ComposeSheet(tutorName: String, busy: Boolean, onDismiss: () -> Unit, onSend: (String, String, () -> Unit) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    TLSheet("New message", "To $tutorName", "Short notices work best — a venue change, what to focus on, a document that is missing.", onDismiss) { close ->
        Field("Topic", topic, { topic = it.take(120) }, placeholder = "Week 9 session focus", capitalize = false)
        Field("Message", body, { body = it.take(2000) }, placeholder = "Write it here", minLines = 4)
        TLButton("Send message", { onSend(topic, body, close) }, loading = busy, buzz = Buzz.CONFIRM)
    }
}

@Composable
private fun NumberSheet(
    title: String, lead: String, label: String, initial: String, button: String, busy: Boolean,
    onDismiss: () -> Unit, onSave: (String, () -> Unit) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    TLSheet("Configuration", title, lead, onDismiss) { close ->
        Field(label, value, { value = it.filter { c -> c.isDigit() || c == '.' }.take(7) }, keyboard = KeyboardType.Decimal)
        TLButton(button, { onSave(value, close) }, loading = busy, buzz = Buzz.CONFIRM)
    }
}


@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddLecturerSheet(busy: Boolean, onDismiss: () -> Unit, onSave: (String, String, List<String>, () -> Unit) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val picked = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    TLSheet("Admin", "Add a lecturer", "They sign in with Google using this email, choose a password, and see the tutors on these modules.", onDismiss) { close ->
        Field("Name", name, { name = it.take(80) }, placeholder = "Dr M. Pretorius", capitalize = true)
        Field("Email", email, { email = it.take(120) }, placeholder = "name@gmail.com", keyboard = KeyboardType.Email)
        Column {
            Text("MODULES", style = TLType.label, modifier = Modifier.padding(start = 1.dp, bottom = 6.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                com.snokonoko.tutor_link.data.ALL_MODULES.forEach { m ->
                    val on = m.code in picked
                    com.snokonoko.tutor_link.ui.components.ModuleChip(m.code, m.name, on = on, small = true) { if (on) picked.remove(m.code) else picked.add(m.code) }
                }
            }
        }
        TLButton("Add lecturer", { onSave(name, email, picked.toList(), close) }, loading = busy, buzz = Buzz.CONFIRM)
    }
}

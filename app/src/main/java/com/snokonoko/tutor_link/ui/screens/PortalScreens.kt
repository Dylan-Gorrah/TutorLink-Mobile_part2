package com.snokonoko.tutor_link.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.data.Me
import com.snokonoko.tutor_link.data.Role
import com.snokonoko.tutor_link.data.SettingsPatch
import com.snokonoko.tutor_link.ui.SessionState
import com.snokonoko.tutor_link.ui.components.BtnStyle
import com.snokonoko.tutor_link.ui.components.Buzz
import com.snokonoko.tutor_link.ui.components.CountBubble
import com.snokonoko.tutor_link.ui.components.EmptyState
import com.snokonoko.tutor_link.ui.components.Field
import com.snokonoko.tutor_link.ui.components.Ic
import com.snokonoko.tutor_link.ui.components.IconBtn
import com.snokonoko.tutor_link.ui.components.ListRow
import com.snokonoko.tutor_link.ui.components.LocalHaptics
import com.snokonoko.tutor_link.ui.components.LocalReduceMotion
import com.snokonoko.tutor_link.ui.components.NavBar
import com.snokonoko.tutor_link.ui.components.Note
import com.snokonoko.tutor_link.ui.components.ProfileCard
import com.snokonoko.tutor_link.ui.components.Rise
import com.snokonoko.tutor_link.ui.components.RowDivider
import com.snokonoko.tutor_link.ui.components.Sect
import com.snokonoko.tutor_link.ui.components.TLButton
import com.snokonoko.tutor_link.ui.components.TLCard
import com.snokonoko.tutor_link.ui.components.TLIcon
import com.snokonoko.tutor_link.ui.components.pressSpring
import com.snokonoko.tutor_link.ui.components.systemAnimationsOff
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TLType
import kotlinx.coroutines.launch

/* ---------- tabs per role (the demo's 5 / 3 / 5) ---------- */

data class TabSpec(val id: String, val icon: Ic, val label: String, val title: String)

fun tabsFor(role: Role): List<TabSpec> = when (role) {
    Role.Tutor -> listOf(
        TabSpec("home", Ic.Cal, "Schedule", "TutorLink"),
        TabSpec("sessions", Ic.CalChk, "Sessions", "Sessions"),
        TabSpec("inbox", Ic.Inbox, "Messages", "Messages"),
        TabSpec("pay", Ic.Wallet, "Pay", "Pay"),
        TabSpec("profile", Ic.User, "Profile", "Profile"),
    )
    Role.Lecturer -> listOf(
        TabSpec("tutors", Ic.People, "Tutors", "My tutors"),
        TabSpec("messages", Ic.Mail, "Messages", "Messages"),
        TabSpec("profile", Ic.User, "Profile", "Profile"),
    )
    Role.Admin -> listOf(
        TabSpec("home", Ic.Board, "Overview", "Overview"),
        TabSpec("apps", Ic.Clip, "Applications", "Applications"),
        TabSpec("tutors", Ic.People, "Tutors", "Tutors"),
        TabSpec("messages", Ic.Mail, "Messages", "Messages"),
        TabSpec("profile", Ic.User, "Settings", "Settings"),
    )
}

/**
 * The signed-in app: nav bar, the current tab, and the bottom tab bar.
 * [tabContent] draws each tab; profile/settings tabs are built here.
 */
@Composable
fun PortalScreen(
    state: SessionState,
    onOpenSettings: () -> Unit,
    onEditModules: () -> Unit,
    onSignOut: () -> Unit,
    tabContent: @Composable (role: Role, tab: String, me: Me, goTab: (String) -> Unit) -> Unit,
) {
    val me = state.me ?: return
    val role = Role.of(me.account.role) ?: return
    val tabs = remember(role) { tabsFor(role) }
    var tab by rememberSaveable(role) { mutableStateOf(tabs.first().id) }
    val spec = tabs.firstOrNull { it.id == tab } ?: tabs.first()
    val unread = if (role == Role.Tutor) me.unread else 0
    val reduce = LocalReduceMotion.current

    Column(Modifier.fillMaxSize().background(TL.Bg)) {
        NavBar(
            spec.title,
            left = if (role == Role.Tutor) ({ IconBtn(Ic.Mail, { tab = "inbox" }, count = unread) }) else null,
        )
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(if (reduce) 1 else 220)) togetherWith fadeOut(tween(if (reduce) 1 else 120)) },
                label = "tab",
            ) { current ->
                if (current == "profile") {
                    ScrollBody {
                        ProfileTab(role, me, onOpenSettings, onEditModules, onSignOut)
                    }
                } else {
                    tabContent(role, current, me) { tab = it }
                }
            }
        }
        TabBar(tabs, tab, counts = mapOf("inbox" to unread)) { tab = it }
    }
}

@Composable
fun ScrollBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 26.dp),
        content = content,
    )
}

@Composable
private fun TabBar(tabs: List<TabSpec>, current: String, counts: Map<String, Int>, onPick: (String) -> Unit) {
    val haptics = LocalHaptics.current
    Column(Modifier.fillMaxWidth().background(TL.Bg.copy(alpha = 0.96f))) {
        HorizontalDivider(color = TL.Line)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 4.dp, end = 4.dp, top = 7.dp, bottom = 6.dp)) {
            tabs.forEach { t ->
                val on = t.id == current
                val src = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .weight(1f)
                        .pressSpring(src, 0.9f)
                        .clickable(interactionSource = src, indication = null) {
                            if (!on) {
                                haptics.buzz(Buzz.TAB)
                                onPick(t.id)
                            }
                        }
                        .padding(top = 6.dp, bottom = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        TLIcon(t.icon, 21, tint = if (on) TL.Navy else TL.Muted2)
                        val c = counts[t.id] ?: 0
                        if (c > 0) CountBubble(c, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp))
                        if (on) {
                            Box(
                                Modifier.align(Alignment.BottomCenter).offset(y = 4.dp).width(14.dp).height(2.dp).background(TL.Gold),
                            )
                        }
                    }
                    Text(t.label, style = TLType.tab.copy(color = if (on) TL.Navy else TL.Muted2), modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

/** Placeholder until the tab is built in Phase 3 / 4. */
@Composable
fun ComingSoon(title: String) {
    EmptyState(title, "This screen is on its way in the next build.", Ic.Clock)
}

/* ---------- profile tabs ---------- */

@Composable
private fun ColumnScope.ProfileTab(role: Role, me: Me, onOpenSettings: () -> Unit, onEditModules: () -> Unit, onSignOut: () -> Unit) {
    val a = me.account
    when (role) {
        Role.Tutor -> {
            Rise(0) { ProfileCard(a.name, "Approved tutor · ${a.studentNumber ?: ""}") }
            Sect("Modules tutored")
            Rise(1) {
                TLCard {
                    me.modules.forEachIndexed { i, m ->
                        if (i > 0) RowDivider()
                        ListRow(m.name, m.code, metaMono = true)
                    }
                }
            }
            Sect("Account")
            Rise(2) {
                TLCard {
                    ListRow("Email", a.email)
                    RowDivider()
                    ListRow("Approved", formatDate(a.approvedAt))
                }
            }
        }
        Role.Lecturer -> {
            Rise(0) { ProfileCard(a.name, "Lecturer · Rosebank College", gold = true) }
            Sect("Modules taught", action = "Edit" to onEditModules)
            Rise(1) {
                TLCard {
                    if (me.modules.isEmpty()) {
                        ListRow("None chosen yet", "Tap Edit to pick the modules you teach.")
                    }
                    me.modules.forEachIndexed { i, m ->
                        if (i > 0) RowDivider()
                        ListRow(m.name, m.code, metaMono = true)
                    }
                }
            }
            Sect("Account")
            Rise(2) { TLCard { ListRow("Email", a.email) } }
        }
        Role.Admin -> {
            Rise(0) { ProfileCard(a.name, "Administrator · Rosebank College") }
        }
    }
    Sect(if (role == Role.Admin) "Personal" else "Settings")
    Rise(3) {
        TLCard {
            ListRow("Settings", "Haptics, animations, message previews, password", onClick = onOpenSettings, leading = { TLIcon(Ic.Sliders, 20) })
        }
    }
    TLButton("Sign out", onSignOut, style = BtnStyle.Outline, modifier = Modifier.padding(top = 18.dp), buzz = 10)
}

fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val d = java.time.LocalDate.parse(iso.take(10))
        d.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    } catch (_: Exception) {
        iso
    }
}

/* ---------- personal settings ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SessionState,
    onBack: () -> Unit,
    onChange: (SettingsPatch) -> Unit,
    onChangePassword: (String, String, () -> Unit) -> Unit,
    onEditModules: () -> Unit,
    onSignOut: () -> Unit,
) {
    val s = state.settings
    val role = Role.of(state.me?.account?.role)
    val systemOff = systemAnimationsOff(LocalContext.current)
    var showPassword by remember { mutableStateOf(false) }

    Page("Settings", onBack = onBack) {
        Sect("How the app feels", first = true)
        TLCard {
            ToggleRow("Haptics", "Vibrate on taps, sends and errors", s.haptics) { onChange(SettingsPatch(haptics = it)) }
            RowDivider()
            ToggleRow("Reduce animations", "Turns off slides, springs and list fade-ins", s.reduceMotion) { onChange(SettingsPatch(reduceMotion = it)) }
            RowDivider()
            ToggleRow("Message previews", "Show the first line under each message topic", s.messagePreviews) { onChange(SettingsPatch(messagePreviews = it)) }
        }
        if (systemOff) {
            Note("Animations are off on this phone, so the app keeps them off too.", modifier = Modifier.padding(top = 10.dp))
        }

        Sect("Account")
        TLCard {
            ListRow("Change password", "Pick a new password for this account", onClick = { showPassword = true })
            if (role == Role.Lecturer) {
                RowDivider()
                ListRow("My modules", state.me?.modules?.joinToString(" · ") { it.code } ?: "", onClick = onEditModules)
            }
        }
        Text(
            "Your settings are saved to your account, so they follow you to any phone you sign in on.",
            style = TLType.small,
            modifier = Modifier.padding(top = 12.dp),
        )
        TLButton("Sign out", onSignOut, style = BtnStyle.Outline, modifier = Modifier.padding(top = 18.dp), buzz = 10)
    }

    if (showPassword) {
        PasswordSheet(busy = state.busy, onDismiss = { showPassword = false }) { p1, p2 ->
            onChangePassword(p1, p2) { showPassword = false }
        }
    }
}

@Composable
private fun ToggleRow(title: String, meta: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = LocalHaptics.current
    ListRow(title, meta, trailing = {
        Switch(
            checked = value,
            onCheckedChange = { haptics.buzz(Buzz.TAB); onChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = TL.Gold,
                checkedTrackColor = TL.Navy,
                checkedBorderColor = TL.Navy,
                uncheckedThumbColor = TL.Muted2,
                uncheckedTrackColor = TL.Bg,
                uncheckedBorderColor = TL.Line,
            ),
        )
    })
}

/* ---------- bottom sheet wrapper (drag down to dismiss) ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TLSheet(sub: String, title: String, lead: String?, onDismiss: () -> Unit, content: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TL.Card,
        scrimColor = TL.Scrim,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 16.dp).width(36.dp).height(4.dp).background(TL.Line, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
        },
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp).navigationBarsPadding()) {
            Text(sub.uppercase(), style = TLType.eyebrow)
            Text(title, style = TLType.sheetTitle, modifier = Modifier.padding(top = 3.dp))
            if (lead != null) Text(lead, style = TLType.small.copy(fontSize = 13.sp), modifier = Modifier.padding(top = 6.dp))
            Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content(close) }
        }
    }
}

@Composable
private fun PasswordSheet(busy: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    TLSheet("Account", "Change password", "At least 6 characters. You stay signed in on this phone.", onDismiss) {
        Field("New password", p1, { p1 = it }, password = true)
        Field("Confirm password", p2, { p2 = it }, password = true, ime = ImeAction.Done, onDone = { onSave(p1, p2) })
        TLButton("Save password", { onSave(p1, p2) }, loading = busy, buzz = Buzz.CONFIRM)
    }
}


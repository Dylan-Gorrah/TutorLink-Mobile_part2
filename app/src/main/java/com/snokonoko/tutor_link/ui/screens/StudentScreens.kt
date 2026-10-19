package com.snokonoko.tutor_link.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.snokonoko.tutor_link.data.ALL_MODULES
import com.snokonoko.tutor_link.data.ApplicantApp
import com.snokonoko.tutor_link.data.moduleName
import com.snokonoko.tutor_link.ui.SessionState
import com.snokonoko.tutor_link.ui.components.Badge
import com.snokonoko.tutor_link.ui.components.BadgeKind
import com.snokonoko.tutor_link.ui.components.BtnStyle
import com.snokonoko.tutor_link.ui.components.Buzz
import com.snokonoko.tutor_link.ui.components.Display
import com.snokonoko.tutor_link.ui.components.Eyebrow
import com.snokonoko.tutor_link.ui.components.Field
import com.snokonoko.tutor_link.ui.components.Ic
import com.snokonoko.tutor_link.ui.components.ListRow
import com.snokonoko.tutor_link.ui.components.LocalHaptics
import com.snokonoko.tutor_link.ui.components.ModuleChip
import com.snokonoko.tutor_link.ui.components.Note
import com.snokonoko.tutor_link.ui.components.NoteKind
import com.snokonoko.tutor_link.ui.components.RowDivider
import com.snokonoko.tutor_link.ui.components.Sect
import com.snokonoko.tutor_link.ui.components.Sub
import com.snokonoko.tutor_link.ui.components.TLButton
import com.snokonoko.tutor_link.ui.components.TLCard
import com.snokonoko.tutor_link.ui.theme.TLType

/* ---------- Student: sign in or create an account ---------- */

@Composable
fun StudentAuthScreen(
    register: Boolean,
    state: SessionState,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onSwitchMode: () -> Unit,
    onRegister: (String, String, String) -> String?,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHaptics.current

    fun submit() {
        if (register) {
            error = onRegister(email, password, confirm)
            if (error != null) haptics.buzz(Buzz.ERROR)
        } else onSignIn(email, password)
    }

    Page("Student", onBack = onBack) {
        Eyebrow(if (register) "Step 1 of 2 · Your account" else "Apply to tutor")
        Display(if (register) "Create your\nstudent account" else "Become a\npeer tutor", small = true)
        Sub(
            if (register) "Then tell us your marks and the modules you'd like to tutor."
            else "Sign in with Google, or with the email and password you registered with.",
        )

        if (!register) {
            TLButton("Continue with Google", onGoogle, style = BtnStyle.Navy, icon = Ic.User, modifier = Modifier.padding(top = 24.dp), enabled = !state.busy)
            Text("or use your email", style = TLType.small, modifier = Modifier.padding(top = 18.dp))
        }

        Column(Modifier.padding(top = if (register) 24.dp else 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Field("Email", email, { email = it; error = null }, placeholder = "you@gmail.com", keyboard = KeyboardType.Email)
            Field(
                "Password", password, { password = it; error = null },
                placeholder = if (register) "At least 6 characters" else "••••••••",
                password = true, ime = if (register) ImeAction.Next else ImeAction.Go, onDone = { if (!register) submit() },
            )
            if (register) {
                Field("Confirm password", confirm, { confirm = it; error = null }, placeholder = "Type it again", password = true, ime = ImeAction.Go, onDone = { submit() }, error = error)
            }
        }
        TLButton(if (register) "Create account" else "Sign in", ::submit, modifier = Modifier.padding(top = 20.dp), loading = state.busy, buzz = 12)
        TLButton(
            if (register) "Already have an account? Sign in" else "New here? Create an account",
            onSwitchMode, style = BtnStyle.Ghost, modifier = Modifier.padding(top = 9.dp),
        )
        Note(
            "Your password is encrypted by Supabase Auth. When the programme approves your application, this same login opens the tutor app.",
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

/* ---------- First Google sign-in: choose a password ---------- */

@Composable
fun ChoosePasswordScreen(state: SessionState, onSave: (String, String) -> Unit, onSignOut: () -> Unit) {
    var p1 by rememberSaveable { mutableStateOf("") }
    var p2 by rememberSaveable { mutableStateOf("") }
    Page("Choose a password") {
        Eyebrow("One more step")
        Display("Choose a\npassword", small = true)
        Sub("You signed in with Google. Pick a password too, so you can also sign in with your email.")
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Field("New password", p1, { p1 = it }, placeholder = "At least 6 characters", password = true)
            Field("Confirm password", p2, { p2 = it }, placeholder = "Type it again", password = true, ime = ImeAction.Go, onDone = { onSave(p1, p2) })
        }
        TLButton("Save and continue", { onSave(p1, p2) }, modifier = Modifier.padding(top = 20.dp), loading = state.busy, buzz = Buzz.CONFIRM)
        TLButton("Sign out", onSignOut, style = BtnStyle.Ghost, modifier = Modifier.padding(top = 9.dp))
    }
}

/* ---------- Student: the application form, then its status ---------- */

@Composable
fun ApplicantScreen(
    state: SessionState,
    onApply: (String, String, String, Map<String, String>) -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    val app = state.applicant?.application
    if (app == null) ApplicationForm(state, onApply, onSignOut) else ApplicationStatus(app, state, onRefresh, onSignOut)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplicationForm(state: SessionState, onApply: (String, String, String, Map<String, String>) -> Unit, onSignOut: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var programme by rememberSaveable { mutableStateOf("Diploma in IT — Software Development") }
    val marks = remember { mutableStateMapOf<String, String>() }

    Page("Apply to tutor") {
        Eyebrow("Step 2 of 2 · Your qualifications")
        Display("Tell us about\nyour marks", small = true)
        Sub("Pick the modules you'd like to tutor and enter your final mark for each. You need at least 75% to be eligible.")
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Field("Full name", name, { name = it }, placeholder = "Naledi Mahlangu", capitalize = true)
            Field("Student number", number, { number = it.uppercase().take(12) }, placeholder = "ST10398445")
            Field("Programme", programme, { programme = it.take(120) })
        }
        Sect("Modules you want to tutor")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            ALL_MODULES.forEach { m ->
                val on = m.code in marks
                ModuleChip(m.code, m.name, on = on) { if (on) marks.remove(m.code) else marks[m.code] = "" }
            }
        }
        if (marks.isNotEmpty()) {
            Sect("Your marks")
            TLCard {
                marks.keys.sorted().forEachIndexed { i, code ->
                    if (i > 0) RowDivider()
                    Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(moduleName(code), style = TLType.rname)
                            Text(code, style = TLType.rmeta.copy(fontFamily = com.snokonoko.tutor_link.ui.theme.Mono))
                        }
                        Field("%", marks[code] ?: "", { marks[code] = it.filter(Char::isDigit).take(3) }, placeholder = "0–100", keyboard = KeyboardType.Number, modifier = Modifier.width(88.dp))
                    }
                }
            }
        }
        TLButton("Submit application", { onApply(name, number, programme, marks.toMap()) }, modifier = Modifier.padding(top = 22.dp), loading = state.busy, buzz = Buzz.DECIDE)
        TLButton("Sign out", onSignOut, style = BtnStyle.Ghost, modifier = Modifier.padding(top = 9.dp))
    }
}

@Composable
private fun ApplicationStatus(app: ApplicantApp, state: SessionState, onRefresh: () -> Unit, onSignOut: () -> Unit) {
    val (label, kind) = when (app.status) {
        "approved" -> "Approved" to BadgeKind.Green
        "rejected" -> "Not approved" to BadgeKind.Red
        "under_review" -> "Under review" to BadgeKind.Amber
        else -> "Pending" to BadgeKind.Slate
    }
    Page("My application") {
        Eyebrow("Application · ${app.id}")
        Display(app.name.substringBefore(' ').ifBlank { "Your application" }, small = true)
        Row(Modifier.padding(top = 12.dp)) { Badge(label, kind) }
        Note(
            buildAnnotatedString {
                when (app.status) {
                    "approved" -> { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("You're in.") }; append(" Tap Check again to open the tutor app.") }
                    "rejected" -> append("The programme office has replied below.")
                    else -> append("The programme office is reviewing your marks. This page updates when they decide.")
                }
            },
            kind = if (app.status == "approved") NoteKind.Green else NoteKind.Gold,
            icon = if (app.status == "approved") Ic.Check else Ic.Clock,
            modifier = Modifier.padding(top = 16.dp),
        )
        Sect("Details")
        TLCard {
            ListRow("Submitted", formatDate(app.submittedAt))
            RowDivider()
            ListRow("Modules", app.appliedModules.joinToString(" · ") { "$it ${moduleName(it)}" })
            if (app.processedAt != null) { RowDivider(); ListRow("Decided", formatDate(app.processedAt)) }
        }
        if (app.responseMessage.isNotBlank()) {
            Sect("Response")
            TLCard(pad = true) { Text(app.responseMessage, style = TLType.body) }
        }
        TLButton("Check again", onRefresh, modifier = Modifier.padding(top = 20.dp), loading = state.busy)
        TLButton("Sign out", onSignOut, style = BtnStyle.Outline, modifier = Modifier.padding(top = 9.dp), buzz = 10)
    }
}

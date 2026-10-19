package com.snokonoko.tutor_link.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.R
import com.snokonoko.tutor_link.data.ALL_MODULES
import com.snokonoko.tutor_link.data.Role
import com.snokonoko.tutor_link.domain.Validators
import com.snokonoko.tutor_link.ui.SessionState
import com.snokonoko.tutor_link.ui.components.Badge
import com.snokonoko.tutor_link.ui.components.BadgeKind
import com.snokonoko.tutor_link.ui.components.BtnStyle
import com.snokonoko.tutor_link.ui.components.Buzz
import com.snokonoko.tutor_link.ui.components.Display
import com.snokonoko.tutor_link.ui.components.Eyebrow
import com.snokonoko.tutor_link.ui.components.Field
import com.snokonoko.tutor_link.ui.components.Ic
import com.snokonoko.tutor_link.ui.components.IconBtn
import com.snokonoko.tutor_link.ui.components.LocalHaptics
import com.snokonoko.tutor_link.ui.components.ModuleChip
import com.snokonoko.tutor_link.ui.components.NavBar
import com.snokonoko.tutor_link.ui.components.Note
import com.snokonoko.tutor_link.ui.components.NoteKind
import com.snokonoko.tutor_link.ui.components.Rise
import com.snokonoko.tutor_link.ui.components.Sub
import com.snokonoko.tutor_link.ui.components.TLButton
import com.snokonoko.tutor_link.ui.components.TLIcon
import com.snokonoko.tutor_link.ui.components.leftEdge
import com.snokonoko.tutor_link.ui.components.pressSpring
import com.snokonoko.tutor_link.ui.theme.R4
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TLType

/** A plain scrolling page under a nav bar, padded like the demo's .pad */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(TL.Bg)) {
        NavBar(title, left = onBack?.let { { IconBtn(Ic.Back, it, size = 24, stroke = 2.1f) } }, right = right)
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 26.dp),
            content = content,
        )
    }
}

/* ---------- role select ---------- */

private data class RoleCard(val role: Role, val icon: Ic, val title: String, val desc: String, val gold: Boolean = false, val tag: String? = null)

private val STUDENT_CARD = RoleCard(Role.Tutor, Ic.Clip, "Student", "Apply to become a peer tutor", tag = "Apply")

private val ROLE_CARDS = listOf(
    RoleCard(Role.Tutor, Ic.Cap, "Tutor", "Sessions, attendance, pay, messages"),
    RoleCard(Role.Lecturer, Ic.Board, "Lecturer", "Your modules and their tutors", gold = true, tag = "New"),
    RoleCard(Role.Admin, Ic.Shield, "Admin", "Applications, tutors, oversight"),
)

@Composable
fun RoleSelectScreen(onPick: (Role) -> Unit, onStudent: () -> Unit) {
    Column(Modifier.fillMaxSize().background(TL.Bg)) {
        Column(Modifier.fillMaxWidth().background(TL.Card).statusBarsPadding()) {
            Image(
                painterResource(R.drawable.rosebank_logo),
                contentDescription = "Rosebank College",
                modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 14.dp).height(40.dp),
            )
            HorizontalDivider(color = TL.Line)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 26.dp),
        ) {
            Eyebrow("Rosebank College")
            Display("TutorLink")
            Sub("The peer tutoring programme, on your phone. Choose how you use it.")
            Spacer(Modifier.height(26.dp))
            ROLE_CARDS.forEachIndexed { i, card ->
                Rise(i) { RoleCardView(card) { onPick(card.role) } }
                Spacer(Modifier.height(10.dp))
            }
            Rise(3) { RoleCardView(STUDENT_CARD, onStudent) }
            Text(
                "Students: apply here with your Google account. Once the programme approves you, the same login opens the tutor app.",
                style = TLType.small,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun RoleCardView(card: RoleCard, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .pressSpring(src)
            .clip(R4)
            .background(TL.Card)
            .border(1.dp, TL.Line, R4)
            .leftEdge(if (card.gold) TL.Gold else TL.Line)
            .clickable(interactionSource = src, indication = null) { haptics.buzz(Buzz.TAP); onClick() }
            .padding(horizontal = 16.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(R4)
                .background(if (card.gold) TL.GoldBg else TL.Bg)
                .border(1.dp, if (card.gold) TL.GoldBd else TL.Line, R4),
            contentAlignment = Alignment.Center,
        ) { TLIcon(card.icon, 22, tint = if (card.gold) TL.GoldD else TL.Navy) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.title, style = TLType.rname.copy(fontSize = 16.sp))
                if (card.tag != null) {
                    Spacer(Modifier.size(8.dp))
                    Badge(card.tag, BadgeKind.Gold)
                }
            }
            Text(card.desc, style = TLType.rmeta.copy(fontSize = 12.5.sp), modifier = Modifier.padding(top = 2.dp))
        }
        TLIcon(Ic.Chev, 18, 2.1f, TL.Muted2)
    }
}

/* ---------- sign in / register ---------- */

private val TEST_LOGIN = mapOf(Role.Tutor to "student1", Role.Lecturer to "lecturer1", Role.Admin to "admin1")

@Composable
fun SignInScreen(
    role: Role,
    register: Boolean,
    state: SessionState,
    onBack: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onSwitchMode: () -> Unit,
    onRegister: (String, String, String, String) -> Validators.RegisterErrors?,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(Validators.RegisterErrors()) }
    val haptics = LocalHaptics.current

    fun submit() {
        if (register) {
            val e = onRegister(name, email, password, confirm)
            errors = e ?: Validators.RegisterErrors()
            if (e != null) haptics.buzz(Buzz.ERROR)
        } else {
            onSignIn(email, password)
        }
    }

    Page(role.label, onBack = onBack) {
        if (register) Eyebrow("Step 1 of 2 · Your details")
        Display(if (register) "Create your\nlecturer account" else "Welcome\nback", small = true)
        Sub(if (register) "Register once, then pick the modules you teach." else "Sign in with your campus account.")

        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (register) {
                Field("Full name", name, { name = it; errors = errors.copy(name = null) }, placeholder = "Dr M. Pretorius", error = errors.name, capitalize = true)
            }
            Field(
                if (register) "Campus email" else "Email or username",
                email,
                { email = it; errors = errors.copy(email = null) },
                placeholder = if (register) "name@rosebank.ac.za" else "name@rosebank.ac.za or ${TEST_LOGIN[role]}",
                error = errors.email,
                keyboard = KeyboardType.Email,
            )
            Field(
                "Password",
                password,
                { password = it; errors = errors.copy(password = null) },
                placeholder = if (register) "At least 6 characters" else "••••••••",
                error = errors.password,
                password = true,
                ime = if (register) ImeAction.Next else ImeAction.Go,
                onDone = { if (!register) submit() },
            )
            if (register) {
                Field("Confirm password", confirm, { confirm = it; errors = errors.copy(confirm = null) }, placeholder = "Type it again", error = errors.confirm, password = true, ime = ImeAction.Go, onDone = { submit() })
            }
        }

        if (register) {
            Note(
                buildAnnotatedString {
                    append("Use your ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("rosebank.ac.za") }
                    append(" address — it is how the programme office identifies you.")
                },
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        TLButton(
            if (register) "Continue" else "Sign in",
            ::submit,
            modifier = Modifier.padding(top = if (register) 18.dp else 24.dp),
            loading = state.busy,
            buzz = 12,
        )

        when {
            register -> TLButton("Already registered? Sign in", onSwitchMode, style = BtnStyle.Ghost, modifier = Modifier.padding(top = 9.dp))
            else -> Note(
                when (role) {
                    Role.Tutor -> "Tutor accounts are created automatically when an application is approved."
                    Role.Lecturer -> "Lecturer accounts are added by the programme admin. The first time, sign in with Google and choose a password."
                    Role.Admin -> "Admin accounts are issued by the programme office."
                },
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (!register) {
            TLButton("Continue with Google", onGoogle, style = BtnStyle.Outline, icon = Ic.User, modifier = Modifier.padding(top = 12.dp), enabled = !state.busy)
            Note(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Test account") }
                    append(" — ${TEST_LOGIN[role]} · password 123456. Tap to fill in.")
                },
                kind = NoteKind.Gold,
                icon = Ic.Sliders,
                modifier = Modifier.padding(top = 14.dp),
                onClick = { email = TEST_LOGIN[role] ?: ""; password = "123456"; haptics.buzz(Buzz.TAB) },
            )
        }
    }
}

/* ---------- lecturer module picker ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModulePickerScreen(
    signup: Boolean,
    initial: List<String>,
    busy: Boolean,
    onBack: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val picking = remember { mutableStateListOf<String>().apply { addAll(initial) } }
    val n = picking.size
    Page(if (signup) "Modules" else "Edit modules", onBack = onBack) {
        Eyebrow(if (signup) "Step 2 of 2 · Your modules" else "Your teaching load")
        Display("Which modules\ndo you teach?", small = true)
        Sub("Tutors on these modules appear on your dashboard, and they are the only tutors you can message.")
        FlowRow(
            Modifier.padding(top = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ALL_MODULES.forEach { m ->
                val on = m.code in picking
                ModuleChip(m.code, m.name, on = on) { if (on) picking.remove(m.code) else picking.add(m.code) }
            }
        }
        TLButton(
            (if (signup) "Create account" else "Save modules") + if (n > 0) " ($n)" else "",
            { onSave(picking.toList()) },
            modifier = Modifier.padding(top = 24.dp),
            enabled = n > 0,
            loading = busy,
            buzz = 12,
        )
        Text(
            if (signup) "Pick at least one. You can change this any time from your profile, and your dashboard updates straight away."
            else "Removing a module hides its tutors and stops you messaging them.",
            style = TLType.small,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

package com.snokonoko.tutor_link.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snokonoko.tutor_link.ui.components.Buzz
import com.snokonoko.tutor_link.ui.components.Display
import com.snokonoko.tutor_link.ui.components.Eyebrow
import com.snokonoko.tutor_link.ui.components.Ic
import com.snokonoko.tutor_link.ui.components.LocalHaptics
import com.snokonoko.tutor_link.ui.components.LocalReduceMotion
import com.snokonoko.tutor_link.ui.components.Note
import com.snokonoko.tutor_link.ui.components.RowDivider
import com.snokonoko.tutor_link.ui.components.Sub
import com.snokonoko.tutor_link.ui.components.TLCard
import com.snokonoko.tutor_link.ui.components.TLIcon
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TLType

/** One help section: a title and a few short answers. Kept in the app, so it works offline. */
data class HelpSection(val id: String, val title: String, val items: List<Pair<String, String>>)

val HELP = listOf(
    HelpSection(
        "start", "Getting in",
        listOf(
            "Which card do I tap?" to "Tutor, Lecturer or Admin if you already have an account. Students who want to become a tutor tap Student.",
            "How do I sign in?" to "Use your email and password, or tap Continue with Google. Test accounts can type just the username, like student1.",
            "First time with Google?" to "You'll be asked to choose a password once, so you can also sign in with your email later.",
            "It says “This is a tutor account”" to "You tapped the wrong card. Go back and choose the role shown in the message.",
            "It says “No TutorLink account for this email”" to "That email hasn't been given an account yet. Students apply with the Student card; lecturers are added by admin.",
        ),
    ),
    HelpSection(
        "student", "Students: applying to tutor",
        listOf(
            "Create an account" to "Tap Student, then Continue with Google, or create an account with your email and a password of at least 6 characters.",
            "Apply" to "Enter your name, student number and programme, pick the modules you'd like to tutor and type your final mark for each. You need 75% or more in each to be eligible.",
            "After applying" to "Your status shows Pending, then Under review. Tap Check again. Once approved, the same login opens the tutor app with a welcome message.",
        ),
    ),
    HelpSection(
        "tutor", "Tutor app",
        listOf(
            "Schedule" to "Your upcoming sessions. Today's session has a Mark button: one tap logs it as Pending.",
            "Sessions" to "Everything you've logged. Tap Log session for a session that isn't on the schedule. Pending turns to Confirmed once admin checks it against your signed register.",
            "Upload a register" to "Sessions → Upload. Pick the module and a PDF or photo of the signed sheet (up to 10 MB).",
            "Pay" to "The gold figure is what's approved (confirmed sessions × the rate). Export statement shares a CSV of your confirmed sessions.",
            "Messages" to "Notices from your lecturer and the programme admin. The number on the mail icon is how many you haven't opened.",
        ),
    ),
    HelpSection(
        "lecturer", "Lecturer app",
        listOf(
            "My tutors" to "Only tutors on the modules you teach appear here. A gold dot means they haven't opened your latest notice.",
            "Send a notice" to "Tap a tutor's name, then New message. Add a topic and the message, then Send. It shows “delivered” until they open it.",
            "Change my modules" to "Profile → Modules taught → Edit. Removing a module hides its tutors and stops you messaging them.",
        ),
    ),
    HelpSection(
        "admin", "Admin app",
        listOf(
            "Overview" to "Open applications, active tutors and money outstanding. The amber strip means tutors have sessions waiting for you to confirm.",
            "Approve an application" to "Applications → tap a name. Check the marks, tap Mark done for the background check, pick a template or write a reply, then Approve. The student's login becomes a tutor account and they get a welcome message.",
            "Confirm and pay" to "Tutors → tap a tutor. Confirm each pending session, use Add session for ones they missed, then Mark as paid.",
            "Message a tutor" to "Messages → New message → pick the tutor → write it.",
            "Rate, minimum mark, lecturers" to "Settings: tap the rate or minimum mark to change it (every money figure updates), or Add a lecturer with their Gmail and modules.",
        ),
    ),
    HelpSection(
        "messages", "Messages: what the dots mean",
        listOf(
            "Gold dot" to "Unread. Tap it to open; the dot and the badge drop.",
            "Navy dot" to "You sent it.",
            "Grey ring" to "Read.",
            "Tags" to "Lecturer (gold), Admin (blue) or You.",
        ),
    ),
    HelpSection(
        "settings", "Settings",
        listOf(
            "Where" to "Profile → Settings (admin: Settings tab → Personal).",
            "Haptics" to "Turns every vibration on or off.",
            "Reduce animations" to "Turns off slides, springs and fade-ins. It's also on automatically if your phone has animations off.",
            "Message previews" to "Shows or hides the first line under each message topic.",
            "Change password" to "At least 6 characters, typed twice.",
        ),
    ),
    HelpSection(
        "trouble", "Troubleshooting",
        listOf(
            "“No internet connection”" to "Check Wi-Fi or mobile data, then pull down on the screen to refresh.",
            "“Those details do not match an account”" to "Check the email and password. Test accounts use the password 123456.",
            "“Your session has expired”" to "Sign in again. Your settings are saved to your account.",
            "Something looks out of date" to "Pull down on any list to refresh it.",
        ),
    ),
)

@Composable
fun HelpScreen(start: String?, onBack: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(start ?: "start") }
    val haptics = LocalHaptics.current
    val reduce = LocalReduceMotion.current
    Page("Help", onBack = onBack) {
        Eyebrow("User guide")
        Display("How TutorLink\nworks", small = true)
        Sub("Tap a section to open it. The ? on each main screen brings you straight to the right part.")
        TLCard(modifier = Modifier.padding(top = 22.dp)) {
            HELP.forEachIndexed { i, s ->
                if (i > 0) RowDivider()
                val isOpen = open == s.id
                Column {
                    Row(
                        Modifier.fillMaxWidth().clickable { haptics.buzz(Buzz.TAB); open = if (isOpen) "" else s.id }.padding(horizontal = 15.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(s.title, style = TLType.rname.copy(color = if (isOpen) TL.Navy else TL.Text), modifier = Modifier.weight(1f))
                        TLIcon(Ic.Chev, 17, 2.1f, TL.Muted2, Modifier.rotate(if (isOpen) 90f else 0f))
                    }
                    AnimatedVisibility(
                        isOpen,
                        enter = if (reduce) fadeIn(tween(1)) else expandVertically(tween(320)) + fadeIn(tween(240)),
                        exit = if (reduce) fadeOut(tween(1)) else shrinkVertically(tween(260)) + fadeOut(tween(160)),
                    ) {
                        Column(Modifier.padding(start = 15.dp, end = 15.dp, bottom = 16.dp)) {
                            s.items.forEach { (q, a) ->
                                Text(q, style = TLType.rname.copy(fontSize = 13.5.sp), modifier = Modifier.padding(top = 10.dp))
                                Text(a, style = TLType.body.copy(fontSize = 13.sp, lineHeight = 21.sp), modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
            }
        }
        Note("Still stuck? Email the programme office at tutoring@rosebank.ac.za.", modifier = Modifier.padding(top = 14.dp))
    }
}

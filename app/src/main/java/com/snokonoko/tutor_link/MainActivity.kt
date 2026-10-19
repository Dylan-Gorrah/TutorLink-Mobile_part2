package com.snokonoko.tutor_link

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import com.snokonoko.tutor_link.ui.AppRoot
import com.snokonoko.tutor_link.ui.SessionViewModel
import com.snokonoko.tutor_link.ui.theme.TL
import com.snokonoko.tutor_link.ui.theme.TutorLinkTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    private val vm: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(TL.Bg.toArgb(), TL.Bg.toArgb()),
            navigationBarStyle = SystemBarStyle.light(TL.Bg.toArgb(), TL.Bg.toArgb()),
        )
        handleAuthLink(intent)
        setContent {
            TutorLinkTheme {
                AppRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthLink(intent)
    }

    // Google SSO returns to tutorlink://auth-callback
    private fun handleAuthLink(intent: Intent?) {
        if (intent?.data?.scheme != "tutorlink") return
        try {
            graph.supabase.handleDeeplinks(intent)
        } catch (_: Exception) {
            vm.toast("Google sign-in didn't complete. Try again.", error = true)
        }
    }
}

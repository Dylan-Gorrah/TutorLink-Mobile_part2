package com.snokonoko.tutor_link.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snokonoko.tutor_link.data.ApiResult
import com.snokonoko.tutor_link.data.Me
import com.snokonoko.tutor_link.data.RegisterLecturerRequest
import com.snokonoko.tutor_link.data.Role
import com.snokonoko.tutor_link.data.SettingsPatch
import com.snokonoko.tutor_link.data.UserSettings
import com.snokonoko.tutor_link.domain.Validators
import com.snokonoko.tutor_link.graph
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase { Starting, SignedOut, SignedIn, Offline }

data class SessionState(
    val phase: Phase = Phase.Starting,
    val me: Me? = null,
    val settings: UserSettings = UserSettings(),
    val busy: Boolean = false,
    /** A lecturer who has just registered sees the first-run note. */
    val firstRun: Boolean = false,
    val offlineMessage: String = "",
)

/** A message for the toast. Errors also get the long 24 ms buzz. */
data class UiMessage(val text: String, val error: Boolean = false)

data class PendingLecturer(val name: String, val email: String, val password: String)

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val g = app.graph

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    /** Step 1 of lecturer registration, held until they pick modules (as in the demo). */
    var pendingLecturer: PendingLecturer? = null
        private set

    private var googleRole: Role? = null

    init {
        viewModelScope.launch {
            g.localSettings.flow.collect { s -> _state.update { it.copy(settings = s) } }
        }
        viewModelScope.launch {
            // Google SSO returns through the deep link and lands here as an authenticated session.
            g.auth.status.collect { st ->
                if (st is SessionStatus.Authenticated && googleRole != null && _state.value.phase != Phase.SignedIn) {
                    val chosen = googleRole
                    googleRole = null
                    loadMe(chosen)
                }
            }
        }
        viewModelScope.launch { restore() }
    }

    private fun say(text: String, error: Boolean = false) {
        _messages.trySend(UiMessage(text, error))
    }

    /** Stay signed in: reuse the saved session if there is one. */
    fun restore() {
        viewModelScope.launch {
            _state.update { it.copy(phase = Phase.Starting) }
            if (g.auth.hasSession()) loadMe(null) else _state.update { it.copy(phase = Phase.SignedOut) }
        }
    }

    private suspend fun loadMe(chosen: Role?) {
        _state.update { it.copy(busy = true) }
        when (val r = g.account.me()) {
            is ApiResult.Ok -> {
                val me = r.value
                val actual = Role.of(me.account.role)
                val mismatch = if (chosen != null && actual != null) Validators.roleMismatch(chosen, actual) else null
                if (actual == null || mismatch != null) {
                    g.auth.signOut()
                    _state.update { it.copy(busy = false, phase = Phase.SignedOut, me = null) }
                    say(mismatch ?: "This account can't use the app.", error = true)
                } else {
                    g.localSettings.save(me.settings)
                    _state.update { it.copy(busy = false, phase = Phase.SignedIn, me = me, settings = me.settings) }
                }
            }
            is ApiResult.Err -> {
                if (r.status == 403 || r.status == 401) {
                    g.auth.signOut()
                    _state.update { it.copy(busy = false, phase = Phase.SignedOut, me = null) }
                    say(if (r.status == 403) "No TutorLink account for this email." else r.message, error = true)
                } else if (chosen == null) {
                    // Starting up with a saved session but no connection.
                    _state.update { it.copy(busy = false, phase = Phase.Offline, offlineMessage = r.message) }
                } else {
                    g.auth.signOut()
                    _state.update { it.copy(busy = false, phase = Phase.SignedOut) }
                    say(r.message, error = true)
                }
            }
        }
    }

    fun refreshMe() {
        viewModelScope.launch {
            val r = g.account.me()
            if (r is ApiResult.Ok) _state.update { it.copy(me = r.value) }
        }
    }

    fun signIn(chosen: Role, login: String, password: String) {
        val problem = Validators.signInError(login, password)
        if (problem != null) {
            say(problem, error = true)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val r = g.auth.signIn(Validators.normaliseLogin(login), password)) {
                is ApiResult.Ok -> loadMe(chosen)
                is ApiResult.Err -> {
                    _state.update { it.copy(busy = false) }
                    say(r.message, error = true)
                }
            }
        }
    }

    fun signInWithGoogle(chosen: Role) {
        viewModelScope.launch {
            googleRole = chosen
            val r = g.auth.startGoogle()
            if (r is ApiResult.Err) {
                googleRole = null
                say(r.message, error = true)
            }
        }
    }

    /** Registration step 1. Returns field errors, or null when they can move to the module picker. */
    fun startRegistration(name: String, email: String, password: String, confirm: String): Validators.RegisterErrors? {
        val errors = Validators.register(name, email, password, confirm)
        if (!errors.ok) return errors
        pendingLecturer = PendingLecturer(name.trim(), email.trim().lowercase(), password)
        return null
    }

    /** Registration step 2: create the account with the chosen modules, then sign in. */
    fun completeRegistration(codes: List<String>) {
        val p = pendingLecturer ?: return
        if (codes.isEmpty()) {
            say("Pick at least one module", error = true)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val r = g.account.registerLecturer(RegisterLecturerRequest(p.name, p.email, p.password, codes))) {
                is ApiResult.Ok -> {
                    pendingLecturer = null
                    when (val s = g.auth.signIn(p.email, p.password)) {
                        is ApiResult.Ok -> {
                            loadMe(Role.Lecturer)
                            _state.update { it.copy(firstRun = true) }
                            say("Account created — welcome to TutorLink")
                        }
                        is ApiResult.Err -> {
                            _state.update { it.copy(busy = false, phase = Phase.SignedOut) }
                            say("Account created. Sign in to continue.")
                        }
                    }
                }
                is ApiResult.Err -> {
                    _state.update { it.copy(busy = false) }
                    say(r.message, error = true)
                }
            }
        }
    }

    fun saveModules(codes: List<String>, onDone: () -> Unit) {
        if (codes.isEmpty()) {
            say("Pick at least one module", error = true)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val r = g.account.saveModules(codes)) {
                is ApiResult.Ok -> {
                    _state.update { s -> s.copy(busy = false, me = s.me?.copy(modules = r.value.modules)) }
                    say("Modules saved")
                    onDone()
                }
                is ApiResult.Err -> {
                    _state.update { it.copy(busy = false) }
                    say(r.message, error = true)
                }
            }
        }
    }

    /** Applies straight away on the phone, then saves through the API. Reverts if the save fails. */
    fun updateSettings(patch: SettingsPatch) {
        val before = _state.value.settings
        val after = before.copy(
            haptics = patch.haptics ?: before.haptics,
            reduceMotion = patch.reduceMotion ?: before.reduceMotion,
            messagePreviews = patch.messagePreviews ?: before.messagePreviews,
        )
        _state.update { it.copy(settings = after) }
        viewModelScope.launch {
            g.localSettings.save(after)
            when (val r = g.account.saveSettings(patch)) {
                is ApiResult.Ok -> _state.update { s -> s.copy(me = s.me?.copy(settings = r.value)) }
                is ApiResult.Err -> {
                    g.localSettings.save(before)
                    _state.update { it.copy(settings = before) }
                    say(r.message, error = true)
                }
            }
        }
    }

    fun changePassword(password: String, confirm: String, onDone: () -> Unit) {
        val problem = Validators.newPassword(password, confirm)
        if (problem != null) {
            say(problem, error = true)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val r = g.auth.changePassword(password)
            _state.update { it.copy(busy = false) }
            when (r) {
                is ApiResult.Ok -> {
                    say("Password changed")
                    onDone()
                }
                is ApiResult.Err -> say(r.message, error = true)
            }
        }
    }

    fun clearFirstRun() = _state.update { it.copy(firstRun = false) }

    fun signOut() {
        viewModelScope.launch {
            g.auth.signOut()
            pendingLecturer = null
            _state.update { it.copy(phase = Phase.SignedOut, me = null, firstRun = false) }
        }
    }

    fun toast(text: String, error: Boolean = false) = say(text, error)
}

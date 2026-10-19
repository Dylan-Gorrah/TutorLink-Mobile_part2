package com.snokonoko.tutor_link.ui.staff

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snokonoko.tutor_link.data.AdminOverview
import com.snokonoko.tutor_link.data.AdminTutor
import com.snokonoko.tutor_link.data.AdminTutorDetail
import com.snokonoko.tutor_link.data.ApiResult
import com.snokonoko.tutor_link.data.AppDetail
import com.snokonoko.tutor_link.data.AppSummary
import com.snokonoko.tutor_link.data.DecisionRequest
import com.snokonoko.tutor_link.data.LecturerTutorsResponse
import com.snokonoko.tutor_link.data.LogSessionRequest
import com.snokonoko.tutor_link.data.MessageDto
import com.snokonoko.tutor_link.data.PostMessageRequest
import com.snokonoko.tutor_link.data.ProgrammePatch
import com.snokonoko.tutor_link.data.ProgrammeSettings
import com.snokonoko.tutor_link.data.Template
import com.snokonoko.tutor_link.data.ThreadResponse
import com.snokonoko.tutor_link.domain.Validators
import com.snokonoko.tutor_link.graph
import com.snokonoko.tutor_link.ui.UiMessage
import com.snokonoko.tutor_link.ui.tutor.Loadable
import com.snokonoko.tutor_link.ui.tutor.mapReady
import com.snokonoko.tutor_link.ui.tutor.toLoadable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffState(
    // lecturer
    val lecturer: Loadable<LecturerTutorsResponse> = Loadable.Loading,
    val sent: Loadable<List<MessageDto>> = Loadable.Loading,
    // admin
    val overview: Loadable<AdminOverview> = Loadable.Loading,
    val applications: Loadable<List<AppSummary>> = Loadable.Loading,
    val tutors: Loadable<List<AdminTutor>> = Loadable.Loading,
    val threads: Loadable<List<MessageDto>> = Loadable.Loading,
    val programme: Loadable<ProgrammeSettings> = Loadable.Loading,
    val templates: Loadable<List<Template>> = Loadable.Loading,
    // detail screens
    val thread: Map<String, Loadable<ThreadResponse>> = emptyMap(),
    val app: Map<String, Loadable<AppDetail>> = emptyMap(),
    val tutor: Map<String, Loadable<AdminTutorDetail>> = emptyMap(),
    val busy: Boolean = false,
    val refreshing: Boolean = false,
)

/** Everything the lecturer and admin apps load and do. */
class StaffViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = app.graph.staff
    private val _state = MutableStateFlow(StaffState())
    val state: StateFlow<StaffState> = _state.asStateFlow()
    private val _toasts = Channel<UiMessage>(Channel.BUFFERED)
    val toasts = _toasts.receiveAsFlow()

    private fun say(text: String, error: Boolean = false) = _toasts.trySend(UiMessage(text, error))
    fun toast(text: String, error: Boolean = false) = say(text, error)

    private fun <T> keepOld(old: Loadable<T>, new: Loadable<T>): Loadable<T> =
        if (new is Loadable.Failed && old is Loadable.Ready) { say(new.message, true); old } else new

    private fun busy(on: Boolean) = _state.update { it.copy(busy = on) }

    /* ---------- loading ---------- */

    fun loadLecturer() = viewModelScope.launch { fetchLecturer() }
    fun loadAdmin() = viewModelScope.launch { fetchOverview(); fetchApps(); fetchTutors(); fetchThreads(); fetchProgramme(); fetchTemplates() }

    fun refresh(role: String) = viewModelScope.launch {
        _state.update { it.copy(refreshing = true) }
        if (role == "lecturer") fetchLecturer() else { fetchOverview(); fetchApps(); fetchTutors(); fetchThreads(); fetchProgramme() }
        _state.update { it.copy(refreshing = false) }
    }

    private suspend fun fetchLecturer() {
        val t = repo.lecturerTutors().toLoadable()
        val s = repo.lecturerThreads().toLoadable().mapReady { it.messages }
        _state.update { it.copy(lecturer = keepOld(it.lecturer, t), sent = keepOld(it.sent, s)) }
    }

    private suspend fun fetchOverview() {
        val r = repo.overview().toLoadable()
        _state.update { it.copy(overview = keepOld(it.overview, r)) }
    }

    private suspend fun fetchApps() {
        val r = repo.applications().toLoadable().mapReady { it.applications }
        _state.update { it.copy(applications = keepOld(it.applications, r)) }
    }

    private suspend fun fetchTutors() {
        val r = repo.tutors().toLoadable().mapReady { it.tutors }
        _state.update { it.copy(tutors = keepOld(it.tutors, r)) }
    }

    private suspend fun fetchThreads() {
        val r = repo.adminThreads().toLoadable().mapReady { it.messages }
        _state.update { it.copy(threads = keepOld(it.threads, r)) }
    }

    private suspend fun fetchProgramme() {
        val r = repo.programme().toLoadable()
        _state.update { it.copy(programme = keepOld(it.programme, r)) }
    }

    private suspend fun fetchTemplates() {
        val r = repo.templates().toLoadable().mapReady { it.templates }
        _state.update { it.copy(templates = keepOld(it.templates, r)) }
    }

    fun loadThread(tutorId: String) = viewModelScope.launch {
        val r = repo.thread(tutorId).toLoadable()
        _state.update { it.copy(thread = it.thread + (tutorId to keepOld(it.thread[tutorId] ?: Loadable.Loading, r))) }
    }

    fun loadApp(id: String) = viewModelScope.launch {
        val r = repo.application(id).toLoadable()
        _state.update { it.copy(app = it.app + (id to keepOld(it.app[id] ?: Loadable.Loading, r))) }
    }

    fun loadTutor(id: String) = viewModelScope.launch {
        val r = repo.tutor(id).toLoadable()
        _state.update { it.copy(tutor = it.tutor + (id to keepOld(it.tutor[id] ?: Loadable.Loading, r))) }
    }

    /* ---------- actions ---------- */

    fun send(tutorId: String, topic: String, body: String, isLecturer: Boolean, onDone: () -> Unit) {
        Validators.message(topic, body)?.let { say(it, true); return }
        viewModelScope.launch {
            busy(true)
            when (val r = repo.send(PostMessageRequest(tutorId, topic.trim(), body.trim()))) {
                is ApiResult.Ok -> {
                    onDone()
                    say("Message sent")
                    loadThread(tutorId)
                    if (isLecturer) fetchLecturer() else fetchThreads()
                }
                is ApiResult.Err -> say(r.message, true)
            }
            busy(false)
        }
    }

    fun backgroundCheck(id: String) = viewModelScope.launch {
        busy(true)
        when (val r = repo.backgroundCheck(id)) {
            is ApiResult.Ok -> {
                _state.update { it.copy(app = it.app + (id to Loadable.Ready(r.value))) }
                say("Background check recorded")
                fetchApps(); fetchOverview()
            }
            is ApiResult.Err -> say(r.message, true)
        }
        busy(false)
    }

    /** Approve creates the tutor's account and drops a welcome message in their thread. */
    fun decide(id: String, approve: Boolean, message: String, onDone: () -> Unit) {
        if (message.isBlank()) { say("Pick or write a response first", true); return }
        viewModelScope.launch {
            busy(true)
            when (val r = repo.decide(id, DecisionRequest(if (approve) "approved" else "rejected", message.trim()))) {
                is ApiResult.Ok -> {
                    _state.update { it.copy(app = it.app + (id to Loadable.Ready(r.value.application))) }
                    onDone()
                    say(if (approve) "${r.value.application.name.substringBefore(' ')} approved" else "Application declined")
                    fetchApps(); fetchOverview(); fetchTutors(); fetchThreads()
                }
                is ApiResult.Err -> say(r.message, true)
            }
            busy(false)
        }
    }

    fun confirm(sessionId: String, tutorId: String) = viewModelScope.launch {
        busy(true)
        when (val r = repo.confirm(sessionId)) {
            is ApiResult.Ok -> { say("Session confirmed"); loadTutor(tutorId); fetchTutors(); fetchOverview() }
            is ApiResult.Err -> say(r.message, true)
        }
        busy(false)
    }

    fun addSession(tutorId: String, date: String, moduleCode: String, students: String, onDone: () -> Unit) {
        val problem = if (date.isBlank() || moduleCode.isBlank()) "Fill in the date, module and student count" else Validators.studentCount(students)
        if (problem != null) { say(problem, true); return }
        viewModelScope.launch {
            busy(true)
            when (val r = repo.addSession(tutorId, LogSessionRequest(date, moduleCode, students.trim().toInt()))) {
                is ApiResult.Ok -> { onDone(); say("Session added"); loadTutor(tutorId); fetchTutors(); fetchOverview() }
                is ApiResult.Err -> say(r.message, true)
            }
            busy(false)
        }
    }

    fun markPaid(tutorId: String, name: String) = viewModelScope.launch {
        busy(true)
        when (val r = repo.markPaid(tutorId)) {
            is ApiResult.Ok -> { say("${name.substringBefore(' ')} marked as paid"); loadTutor(tutorId); fetchTutors(); fetchOverview() }
            is ApiResult.Err -> say(r.message, true)
        }
        busy(false)
    }

    /** Admin adds a lecturer; they sign in with Google the first time and choose a password. */
    fun addLecturer(name: String, email: String, modules: List<String>, onDone: () -> Unit) {
        val problem = when {
            name.isBlank() -> "Enter the lecturer's name"
            !Validators.isEmail(email) -> "Enter a valid email address"
            modules.isEmpty() -> "Pick at least one module"
            else -> null
        }
        if (problem != null) { say(problem, true); return }
        viewModelScope.launch {
            busy(true)
            when (val r = app.graph.account.addLecturer(com.snokonoko.tutor_link.data.AddLecturerRequest(name.trim(), email.trim().lowercase(), modules))) {
                is ApiResult.Ok -> { onDone(); say("Lecturer added — they can sign in with Google") }
                is ApiResult.Err -> say(r.message, true)
            }
            busy(false)
        }
    }

    fun saveRate(text: String, onDone: () -> Unit) {
        Validators.rate(text)?.let { say(it, true); return }
        saveProgramme(ProgrammePatch(ratePerSession = text.trim().toDouble()), "Rate saved", onDone)
    }

    fun saveMinMark(text: String, onDone: () -> Unit) {
        Validators.minMark(text)?.let { say(it, true); return }
        saveProgramme(ProgrammePatch(minMark = text.trim().toDouble()), "Minimum mark saved", onDone)
    }

    private fun saveProgramme(p: ProgrammePatch, done: String, onDone: () -> Unit) = viewModelScope.launch {
        busy(true)
        when (val r = repo.saveProgramme(p)) {
            is ApiResult.Ok -> {
                _state.update { it.copy(programme = Loadable.Ready(r.value)) }
                onDone(); say(done)
                fetchOverview(); fetchTutors(); fetchApps()
            }
            is ApiResult.Err -> say(r.message, true)
        }
        busy(false)
    }
}

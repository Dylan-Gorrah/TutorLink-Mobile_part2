package com.snokonoko.tutor_link.ui.tutor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snokonoko.tutor_link.data.ApiResult
import com.snokonoko.tutor_link.data.LogSessionRequest
import com.snokonoko.tutor_link.data.MessageDto
import com.snokonoko.tutor_link.data.PayResponse
import com.snokonoko.tutor_link.data.RegisterDto
import com.snokonoko.tutor_link.data.SaveRegisterRequest
import com.snokonoko.tutor_link.data.ScheduleResponse
import com.snokonoko.tutor_link.data.SessionDto
import com.snokonoko.tutor_link.domain.MessageLogic
import com.snokonoko.tutor_link.domain.Validators
import com.snokonoko.tutor_link.graph
import com.snokonoko.tutor_link.ui.UiMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Loading / loaded / failed, for every list on screen. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val data: T) : Loadable<T>
    data class Failed(val message: String) : Loadable<Nothing>
}

fun <T> ApiResult<T>.toLoadable(): Loadable<T> = when (this) {
    is ApiResult.Ok -> Loadable.Ready(value)
    is ApiResult.Err -> Loadable.Failed(message)
}

data class TutorState(
    val schedule: Loadable<ScheduleResponse> = Loadable.Loading,
    val sessions: Loadable<List<SessionDto>> = Loadable.Loading,
    val registers: Loadable<List<RegisterDto>> = Loadable.Loading,
    val pay: Loadable<PayResponse> = Loadable.Loading,
    val messages: Loadable<List<MessageDto>> = Loadable.Loading,
    val busy: Boolean = false,
    val refreshing: Boolean = false,
)

class TutorViewModel(app: Application) : AndroidViewModel(app) {
    private val g = app.graph
    private val _state = MutableStateFlow(TutorState())
    val state: StateFlow<TutorState> = _state.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val toasts = _messages.receiveAsFlow()

    /** Unread count changes (the header badge and the Messages tab). */
    private val _unread = MutableStateFlow<Int?>(null)
    val unread: StateFlow<Int?> = _unread.asStateFlow()

    private fun say(text: String, error: Boolean = false) = _messages.trySend(UiMessage(text, error))

    fun loadAll() {
        loadSchedule(); loadSessions(); loadRegisters(); loadPay(); loadMessages()
    }

    fun refresh(tab: String) {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            when (tab) {
                "home" -> fetchSchedule()
                "sessions" -> { fetchSessions(); fetchRegisters() }
                "pay" -> fetchPay()
                "inbox" -> fetchMessages()
            }
            _state.update { it.copy(refreshing = false) }
        }
    }

    fun loadSchedule() = viewModelScope.launch { fetchSchedule() }
    fun loadSessions() = viewModelScope.launch { fetchSessions() }
    fun loadRegisters() = viewModelScope.launch { fetchRegisters() }
    fun loadPay() = viewModelScope.launch { fetchPay() }
    fun loadMessages() = viewModelScope.launch { fetchMessages() }

    private suspend fun fetchSchedule() {
        val r = g.tutor.schedule()
        if (r is ApiResult.Ok) _unread.value = r.value.unread
        _state.update { s -> s.copy(schedule = keepOld(s.schedule, r.toLoadable())) }
    }

    private suspend fun fetchSessions() {
        val r = g.tutor.sessions()
        _state.update { s -> s.copy(sessions = keepOld(s.sessions, r.toLoadable().mapReady { it.sessions })) }
    }

    private suspend fun fetchRegisters() {
        val r = g.tutor.registers()
        _state.update { s -> s.copy(registers = keepOld(s.registers, r.toLoadable().mapReady { it.registers })) }
    }

    private suspend fun fetchPay() {
        val r = g.tutor.pay()
        _state.update { s -> s.copy(pay = keepOld(s.pay, r.toLoadable())) }
    }

    private suspend fun fetchMessages() {
        val r = g.tutor.messages()
        if (r is ApiResult.Ok) _unread.value = MessageLogic.unreadCount(r.value.messages)
        _state.update { s -> s.copy(messages = keepOld(s.messages, r.toLoadable().mapReady { it.messages })) }
    }

    /** A failed refresh keeps what's already on screen and shows a toast instead. */
    private fun <T> keepOld(old: Loadable<T>, new: Loadable<T>): Loadable<T> =
        if (new is Loadable.Failed && old is Loadable.Ready) {
            say(new.message, error = true)
            old
        } else new

    /** Mark today's session: logged as pending until admin confirms it. */
    fun mark(scheduleId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val r = g.tutor.mark(scheduleId)) {
                is ApiResult.Ok -> {
                    say("Attendance logged — pending confirmation")
                    fetchSchedule(); fetchSessions(); fetchPay()
                }
                is ApiResult.Err -> say(r.message, error = true)
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun logSession(date: String, moduleCode: String, students: String, onDone: () -> Unit) {
        val problem = when {
            date.isBlank() || moduleCode.isBlank() -> "Fill in the date, module and student count"
            else -> Validators.studentCount(students)
        }
        if (problem != null) {
            say(problem, error = true)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val r = g.tutor.logSession(LogSessionRequest(date, moduleCode, students.trim().toInt()))) {
                is ApiResult.Ok -> {
                    onDone()
                    say("Session logged")
                    fetchSessions(); fetchSchedule(); fetchPay()
                }
                is ApiResult.Err -> say(r.message, error = true)
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun uploadRegister(accountId: String, moduleCode: String, fileName: String, mimeType: String, bytes: ByteArray, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val up = g.uploader.upload(accountId, fileName, mimeType, bytes)) {
                is ApiResult.Ok -> when (val r = g.tutor.saveRegister(SaveRegisterRequest(moduleCode, fileName, up.value))) {
                    is ApiResult.Ok -> {
                        onDone()
                        say("Register uploaded")
                        fetchRegisters()
                    }
                    is ApiResult.Err -> say(r.message, error = true)
                }
                is ApiResult.Err -> say(up.message, error = true)
            }
            _state.update { it.copy(busy = false) }
        }
    }

    /** Opening an unread notice marks it read: the gold dot and the badge drop straight away. */
    fun open(m: MessageDto) {
        if (m.readByTutor) return
        _state.update { s ->
            val list = (s.messages as? Loadable.Ready)?.data ?: return@update s
            s.copy(messages = Loadable.Ready(list.map { if (it.id == m.id) it.copy(readByTutor = true) else it }))
        }
        _unread.update { (it ?: 1).minus(1).coerceAtLeast(0) }
        viewModelScope.launch {
            when (val r = g.tutor.markRead(m.id)) {
                is ApiResult.Ok -> _unread.value = r.value.unread
                is ApiResult.Err -> say(r.message, error = true)
            }
        }
    }

    fun toast(text: String, error: Boolean = false) = say(text, error)
}

fun <T, R> Loadable<T>.mapReady(f: (T) -> R): Loadable<R> = when (this) {
    is Loadable.Ready -> Loadable.Ready(f(data))
    is Loadable.Failed -> this
    Loadable.Loading -> Loadable.Loading
}

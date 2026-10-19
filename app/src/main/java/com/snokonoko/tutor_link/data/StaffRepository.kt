package com.snokonoko.tutor_link.data

import kotlinx.serialization.Serializable

/* ---------- lecturer ---------- */

@Serializable
data class LastMessage(val topic: String, val sentAt: String)

@Serializable
data class LecturerTutor(
    val id: String,
    val name: String,
    val email: String = "",
    val modules: List<String> = emptyList(),
    val lastMessage: LastMessage? = null,
    val unread: Boolean = false,
)

@Serializable
data class LecturerCounts(val tutors: Int = 0, val modules: Int = 0, val sessions: Int = 0)

@Serializable
data class LecturerTutorsResponse(
    val modules: List<ModuleDto> = emptyList(),
    val counts: LecturerCounts = LecturerCounts(),
    val hasSentMessage: Boolean = false,
    val tutors: List<LecturerTutor> = emptyList(),
)

/* ---------- threads ---------- */

@Serializable
data class ThreadTutor(val id: String, val name: String, val modules: List<String> = emptyList())

@Serializable
data class ThreadResponse(val tutor: ThreadTutor, val messages: List<MessageDto>)

@Serializable
data class PostMessageRequest(val tutorId: String, val topic: String, val body: String)

@Serializable
data class MessageResponse(val message: MessageDto)

/* ---------- admin ---------- */

@Serializable
data class AdminOverview(
    val openApplications: Int = 0,
    val activeTutors: Int = 0,
    val outstanding: Double = 0.0,
    val awaitingConfirmation: Int = 0,
    val rate: Double = 150.0,
)

@Serializable
data class AppSummary(
    val id: String,
    val name: String,
    val email: String = "",
    val studentNumber: String = "",
    val status: String,
    val submittedAt: String = "",
    val appliedModules: List<String> = emptyList(),
    val flag: String? = null,
)

@Serializable
data class ApplicationsResponse(val applications: List<AppSummary>)

@Serializable
data class MarkDto(val code: String, val name: String, val mark: Int, val result: String = "", val appliedFor: Boolean = false)

@Serializable
data class AppDetail(
    val id: String,
    val name: String,
    val email: String = "",
    val studentNumber: String = "",
    val programme: String? = null,
    val status: String,
    val backgroundChecked: Boolean = false,
    val responseMessage: String = "",
    val submittedAt: String = "",
    val processedAt: String? = null,
    val appliedModules: List<String> = emptyList(),
    val minMark: Double = 75.0,
    val flag: String? = null,
    val marks: List<MarkDto> = emptyList(),
)

@Serializable
data class DecisionRequest(val status: String, val message: String)

@Serializable
data class NewTutor(val id: String, val name: String, val email: String)

@Serializable
data class DecisionResponse(val application: AppDetail, val tutor: NewTutor? = null)

@Serializable
data class AdminTutor(
    val id: String,
    val name: String,
    val email: String = "",
    val paid: Boolean = false,
    val modules: List<String> = emptyList(),
    val owed: Double = 0.0,
)

@Serializable
data class AdminTutorsResponse(val tutors: List<AdminTutor>)

@Serializable
data class TutorInfo(
    val id: String,
    val name: String,
    val email: String = "",
    val studentNumber: String? = null,
    val approvedAt: String? = null,
    val paid: Boolean = false,
)

@Serializable
data class TutorPaySummary(val rate: Double = 150.0, val sessions: Int = 0, val confirmedUnpaid: Int = 0, val pending: Int = 0, val owed: Double = 0.0)

@Serializable
data class AdminTutorDetail(
    val tutor: TutorInfo,
    val modules: List<ModuleDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val registers: List<RegisterDto> = emptyList(),
    val pay: TutorPaySummary = TutorPaySummary(),
)

@Serializable
data class MarkPaidResponse(val ok: Boolean = true, val sessionsPaid: Int = 0)

@Serializable
data class ProgrammeSettings(val ratePerSession: Double, val minMark: Double, val other: Map<String, String> = emptyMap())

@Serializable
data class ProgrammePatch(val ratePerSession: Double? = null, val minMark: Double? = null)

@Serializable
data class Template(val id: Int, val label: String, val body: String)

@Serializable
data class TemplatesResponse(val templates: List<Template>)

/** Lecturer and admin endpoints. */
class StaffRepository(private val api: ApiClient) {
    suspend fun lecturerTutors(): ApiResult<LecturerTutorsResponse> = api.get("/lecturer/tutors")
    suspend fun lecturerThreads(): ApiResult<MessagesResponse> = api.get("/lecturer/threads")
    suspend fun thread(tutorId: String): ApiResult<ThreadResponse> = api.get("/messages/thread/$tutorId")
    suspend fun send(req: PostMessageRequest): ApiResult<MessageResponse> = api.post("/messages", req)

    suspend fun overview(): ApiResult<AdminOverview> = api.get("/admin/overview")
    suspend fun applications(status: String = "all"): ApiResult<ApplicationsResponse> = api.get("/admin/applications?status=$status")
    suspend fun application(id: String): ApiResult<AppDetail> = api.get("/admin/applications/$id")
    suspend fun backgroundCheck(id: String): ApiResult<AppDetail> = api.post("/admin/applications/$id/background-check")
    suspend fun decide(id: String, req: DecisionRequest): ApiResult<DecisionResponse> = api.post("/admin/applications/$id/decision", req)
    suspend fun tutors(): ApiResult<AdminTutorsResponse> = api.get("/admin/tutors")
    suspend fun tutor(id: String): ApiResult<AdminTutorDetail> = api.get("/admin/tutors/$id")
    suspend fun confirm(sessionId: String): ApiResult<SessionResponse> = api.post("/admin/sessions/$sessionId/confirm")
    suspend fun addSession(tutorId: String, req: LogSessionRequest): ApiResult<SessionResponse> = api.post("/admin/tutors/$tutorId/sessions", req)
    suspend fun markPaid(tutorId: String): ApiResult<MarkPaidResponse> = api.post("/admin/tutors/$tutorId/mark-paid")
    suspend fun adminThreads(): ApiResult<MessagesResponse> = api.get("/admin/threads")
    suspend fun programme(): ApiResult<ProgrammeSettings> = api.get("/admin/programme-settings")
    suspend fun saveProgramme(p: ProgrammePatch): ApiResult<ProgrammeSettings> = api.put("/admin/programme-settings", p)
    suspend fun templates(): ApiResult<TemplatesResponse> = api.get("/admin/templates")
}

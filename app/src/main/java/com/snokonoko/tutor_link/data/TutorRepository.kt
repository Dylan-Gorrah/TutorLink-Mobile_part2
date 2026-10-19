package com.snokonoko.tutor_link.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/* ---------- shapes from the tutor endpoints ---------- */

@Serializable
data class SessionDto(
    val id: String,
    val tutorId: String = "",
    val moduleCode: String,
    val date: String,
    val studentCount: Int,
    val confirmed: Boolean,
    val paid: Boolean = false,
    val loggedBy: String = "tutor",
)

@Serializable
data class UpcomingDto(
    val id: String,
    val moduleCode: String,
    val day: String,
    val time: String,
    val venue: String,
    val studentCount: Int,
    val live: Boolean,
    val marked: Boolean,
)

@Serializable
data class MonthDto(val sessions: Int = 0, val confirmed: Int = 0, val earned: Double = 0.0)

@Serializable
data class ScheduleResponse(
    val modules: List<ModuleDto> = emptyList(),
    val unread: Int = 0,
    val upcoming: List<UpcomingDto> = emptyList(),
    val month: MonthDto = MonthDto(),
    val pending: List<SessionDto> = emptyList(),
)

@Serializable
data class SessionsResponse(val sessions: List<SessionDto>)

@Serializable
data class SessionResponse(val session: SessionDto)

@Serializable
data class RegisterDto(
    val id: String,
    val moduleCode: String,
    val fileName: String,
    val filePath: String? = null,
    val uploadedAt: String,
)

@Serializable
data class RegistersResponse(val registers: List<RegisterDto>)

@Serializable
data class RegisterResponse(val register: RegisterDto)

@Serializable
data class PayLine(
    val id: String,
    val moduleCode: String,
    val date: String,
    val studentCount: Int,
    val paid: Boolean = false,
    val amount: Double,
)

@Serializable
data class PayResponse(
    val rate: Double,
    val approved: Double,
    val confirmedCount: Int,
    val pendingAmount: Double,
    val projected: Double,
    val paid: Boolean = false,
    val lines: List<PayLine> = emptyList(),
)

@Serializable
data class MessageDto(
    val id: String,
    val tutorId: String,
    val tutorName: String? = null,
    val senderId: String,
    val senderRole: String,
    val senderName: String,
    val topic: String,
    val body: String,
    val sentAt: String,
    val readByTutor: Boolean,
)

@Serializable
data class MessagesResponse(val messages: List<MessageDto>)

@Serializable
data class ReadResponse(val ok: Boolean = true, val unread: Int = 0)

@Serializable
data class LogSessionRequest(val date: String, val moduleCode: String, val studentCount: Int)

@Serializable
data class SaveRegisterRequest(val moduleCode: String, val fileName: String, val filePath: String?)

/* ---------- repository ---------- */

class TutorRepository(private val api: ApiClient) {
    suspend fun schedule(): ApiResult<ScheduleResponse> = api.get("/tutor/schedule")
    suspend fun mark(scheduleId: String): ApiResult<SessionResponse> = api.post("/tutor/sessions/$scheduleId/mark")
    suspend fun sessions(): ApiResult<SessionsResponse> = api.get("/tutor/sessions")
    suspend fun logSession(req: LogSessionRequest): ApiResult<SessionResponse> = api.post("/tutor/sessions", req)
    suspend fun registers(): ApiResult<RegistersResponse> = api.get("/tutor/registers")
    suspend fun saveRegister(req: SaveRegisterRequest): ApiResult<RegisterResponse> = api.post("/tutor/registers", req)
    suspend fun pay(): ApiResult<PayResponse> = api.get("/tutor/pay")
    suspend fun messages(): ApiResult<MessagesResponse> = api.get("/tutor/messages")
    suspend fun markRead(messageId: String): ApiResult<ReadResponse> = api.patch("/messages/$messageId/read")
}

/** Puts a signed register in Supabase Storage under <account id>/, as the storage policy requires. */
class RegisterUploader(private val client: SupabaseClient) {
    suspend fun upload(accountId: String, fileName: String, mimeType: String, bytes: ByteArray): ApiResult<String> = try {
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(80)
        val path = "$accountId/${System.currentTimeMillis()}_$safe"
        client.storage.from("attendance-registers").upload(path, bytes) {
            upsert = false
            contentType = io.ktor.http.ContentType.parse(mimeType)
        }
        ApiResult.Ok(path)
    } catch (e: CancellationException) {
        throw e
    } catch (_: java.io.IOException) {
        ApiResult.Err(Errors.NO_INTERNET, 0)
    } catch (_: Exception) {
        ApiResult.Err("The register couldn't be uploaded. Try again.", -1)
    }
}

package com.snokonoko.tutor_link.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Sign-in, Google SSO, password changes and sign-out go straight to Supabase Auth. */
class AuthRepository(private val client: SupabaseClient) {
    val status: StateFlow<SessionStatus> get() = client.auth.sessionStatus

    suspend fun hasSession(): Boolean = try {
        client.auth.awaitInitialization()
        client.auth.currentSessionOrNull() != null
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    suspend fun token(): String? = try {
        client.auth.currentAccessTokenOrNull()
    } catch (_: Exception) {
        null
    }

    suspend fun signIn(email: String, password: String): ApiResult<Unit> = guard {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun startGoogle(): ApiResult<Unit> = guard { client.auth.signInWith(Google) }

    suspend fun changePassword(newPassword: String): ApiResult<Unit> = guard {
        client.auth.updateUser {
            password = newPassword
            data = buildJsonObject { put("password_set", true) }
        }
    }

    /** False for someone who has only ever signed in with Google and hasn't chosen a password yet. */
    fun passwordSet(): Boolean = try {
        val meta = client.auth.currentUserOrNull()?.userMetadata
        meta?.get("password_set")?.jsonPrimitive?.booleanOrNull == true
    } catch (_: Exception) {
        true
    }

    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Signing out locally still works when the network is down.
            try { client.auth.clearSession() } catch (_: Exception) {}
        }
    }

    private suspend fun <T> guard(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Ok(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: AuthRestException) {
        ApiResult.Err(authMessage(e), e.statusCode)
    } catch (_: HttpRequestException) {
        ApiResult.Err(Errors.NO_INTERNET, 0)
    } catch (_: java.io.IOException) {
        ApiResult.Err(Errors.NO_INTERNET, 0)
    } catch (_: Exception) {
        ApiResult.Err(Errors.GENERIC, -1)
    }

    private fun authMessage(e: AuthRestException): String {
        val m = (e.errorDescription ?: e.message ?: "").lowercase()
        return when {
            "invalid login" in m || "invalid credentials" in m -> "Those details do not match an account."
            "same" in m && "password" in m -> "Choose a password you haven't used before."
            "weak" in m || ("password" in m && "characters" in m) -> "Password needs at least 6 characters."
            "rate" in m || "too many" in m -> "Too many attempts. Wait a minute and try again."
            else -> e.errorDescription ?: Errors.GENERIC
        }
    }
}

/** Profile, personal settings and lecturer modules, all through our API. */
class AccountRepository(private val api: ApiClient) {
    suspend fun me(): ApiResult<Me> = api.get("/me")

    suspend fun saveSettings(patch: SettingsPatch): ApiResult<UserSettings> = api.put("/settings", patch)

    suspend fun registerLecturer(req: RegisterLecturerRequest): ApiResult<RegisterLecturerResponse> =
        api.post("/lecturer/register", req, public = true)

    suspend fun saveModules(codes: List<String>): ApiResult<ModulesResponse> =
        api.put("/lecturer/modules", ModulesRequest(codes))

    suspend fun registerApplicant(email: String, password: String): ApiResult<RegisterLecturerResponse> =
        api.post("/applicant/register", ApplicantRegisterRequest(email, password), public = true)

    suspend fun applicantStatus(): ApiResult<ApplicantStatus> = api.get("/applicant/status")

    suspend fun apply(req: ApplyRequest): ApiResult<ApplicantStatus> = api.post("/applicant/apply", req)

    suspend fun addLecturer(req: AddLecturerRequest): ApiResult<RegisterLecturerResponse> = api.post("/admin/lecturers", req)
}

private val Context.dataStore by preferencesDataStore("tutorlink_settings")

/** A copy of the personal settings on the phone, so haptics and motion apply before the network answers. */
class LocalSettings(private val context: Context) {
    private val haptics = booleanPreferencesKey("haptics")
    private val reduceMotion = booleanPreferencesKey("reduce_motion")
    private val previews = booleanPreferencesKey("message_previews")

    val flow: Flow<UserSettings> = context.dataStore.data.map {
        UserSettings(
            haptics = it[haptics] ?: true,
            reduceMotion = it[reduceMotion] ?: false,
            messagePreviews = it[previews] ?: true,
        )
    }

    suspend fun save(s: UserSettings) {
        try {
            context.dataStore.edit {
                it[haptics] = s.haptics
                it[reduceMotion] = s.reduceMotion
                it[previews] = s.messagePreviews
            }
        } catch (_: Exception) {
            // Disk full or similar: the server copy is still the source of truth.
        }
    }
}

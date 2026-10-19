package com.snokonoko.tutor_link.data

import kotlinx.serialization.Serializable

// JSON shapes returned by the api Edge Function (supabase/functions/api/index.ts).

@Serializable
data class ModuleDto(val code: String, val name: String)

@Serializable
data class AccountDto(
    val id: String,
    val role: String,
    val name: String,
    val email: String,
    val studentNumber: String? = null,
    val approvedAt: String? = null,
    val paid: Boolean = false,
)

@Serializable
data class UserSettings(
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val messagePreviews: Boolean = true,
)

@Serializable
data class Me(
    val account: AccountDto,
    val modules: List<ModuleDto> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val unread: Int = 0,
)

@Serializable
data class SettingsPatch(
    val haptics: Boolean? = null,
    val reduceMotion: Boolean? = null,
    val messagePreviews: Boolean? = null,
)

@Serializable
data class RegisterLecturerRequest(
    val name: String,
    val email: String,
    val password: String,
    val modules: List<String>,
)

@Serializable
data class RegisterLecturerResponse(val ok: Boolean = true)

@Serializable
data class ModulesRequest(val modules: List<String>)

@Serializable
data class ModulesResponse(val modules: List<ModuleDto>)

@Serializable
data class ApiErrorBody(val error: String? = null)

enum class Role(val key: String, val label: String) {
    Tutor("tutor", "Tutor"),
    Lecturer("lecturer", "Lecturer"),
    Admin("admin", "Admin");

    companion object {
        fun of(key: String?): Role? = entries.firstOrNull { it.key == key }
    }
}

/** The demo's module list, used by the lecturer module picker. The API checks the codes again. */
val ALL_MODULES = listOf(
    ModuleDto("DATA6211", "Database (Introduction)"),
    ModuleDto("ISEC6321", "Information Security"),
    ModuleDto("PROG6221", "Programming 2A"),
    ModuleDto("SAND6221", "System Analysis & Design"),
    ModuleDto("DATA6222", "Database (Intermediate)"),
    ModuleDto("HCIN6222", "Human Computer Interaction"),
    ModuleDto("IPMA6212", "IT Project Management"),
    ModuleDto("PROG6212", "Programming 2B"),
    ModuleDto("PROG1111", "Programming 1"),
    ModuleDto("NETW1111", "Networking Fundamentals"),
    ModuleDto("MATH1111", "Mathematics for IT"),
)

fun moduleName(code: String): String = ALL_MODULES.firstOrNull { it.code == code }?.name ?: code

/* ---------- students applying in the app ---------- */

@Serializable
data class ApplicantRegisterRequest(val email: String, val password: String)

@Serializable
data class ApplicantApp(
    val id: String,
    val name: String,
    val status: String,
    val submittedAt: String = "",
    val processedAt: String? = null,
    val responseMessage: String = "",
    val appliedModules: List<String> = emptyList(),
)

@Serializable
data class ApplicantStatus(val email: String = "", val application: ApplicantApp? = null)

@Serializable
data class MarkInput(val code: String, val mark: Int)

@Serializable
data class ApplyRequest(val name: String, val studentNumber: String, val programme: String, val marks: List<MarkInput>)

@Serializable
data class AddLecturerRequest(val name: String, val email: String, val modules: List<String>)

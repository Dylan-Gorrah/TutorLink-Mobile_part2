package com.snokonoko.tutor_link.domain

import com.snokonoko.tutor_link.data.Role

/** Input rules. The API checks all of these again on the server. Wording follows the demo. */
object Validators {
    const val CAMPUS = "rosebank.ac.za"
    const val DEMO_DOMAIN = "tutorlink.demo"
    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE)

    /** "student1" signs in as student1@tutorlink.demo; full emails pass through. */
    fun normaliseLogin(input: String): String {
        val t = input.trim().lowercase()
        return if (t.isEmpty() || '@' in t) t else "$t@$DEMO_DOMAIN"
    }

    fun isEmail(s: String) = EMAIL.matches(s.trim())

    fun isCampusEmail(s: String): Boolean {
        val domain = s.trim().lowercase().substringAfter('@', "")
        return domain == CAMPUS || domain.endsWith(".$CAMPUS")
    }

    fun signInError(login: String, password: String): String? = when {
        login.isBlank() -> "Enter your email or username"
        !isEmail(normaliseLogin(login)) -> "Enter a valid email address"
        password.isEmpty() -> "Enter your password"
        else -> null
    }

    data class RegisterErrors(
        val name: String? = null,
        val email: String? = null,
        val password: String? = null,
        val confirm: String? = null,
    ) {
        val ok get() = name == null && email == null && password == null && confirm == null
        val first get() = name ?: email ?: password ?: confirm
    }

    fun register(name: String, email: String, password: String, confirm: String) = RegisterErrors(
        name = if (name.isBlank()) "Enter your full name" else null,
        email = when {
            !isEmail(email) -> "Enter a valid email address"
            !isCampusEmail(email) -> "Use your rosebank.ac.za campus address"
            else -> null
        },
        password = passwordError(password),
        confirm = if (password.length >= 6 && password != confirm) "The two passwords do not match" else null,
    )

    fun passwordError(password: String): String? =
        if (password.length < 6) "Password needs at least 6 characters" else null

    fun newPassword(password: String, confirm: String): String? =
        passwordError(password) ?: if (password != confirm) "The two passwords do not match" else null

    /** The wrong-card check: a tutor who taps Admin is told which account they have. */
    fun roleMismatch(chosen: Role, actual: Role): String? =
        if (chosen == actual) null else "This is a ${actual.label.lowercase()} account. Go back and choose ${actual.label}."

    fun studentCount(text: String): String? {
        val n = text.trim().toIntOrNull() ?: return "Enter how many students came"
        return if (n in 1..60) null else "Student count must be from 1 to 60"
    }

    fun rate(text: String): String? {
        val v = text.trim().toDoubleOrNull() ?: return "Enter a valid rate"
        return if (v in 50.0..1000.0) null else "Rate must be between R50 and R1000"
    }

    fun minMark(text: String): String? {
        val v = text.trim().toDoubleOrNull() ?: return "Enter a valid mark"
        return if (v in 50.0..100.0) null else "Minimum mark must be between 50 and 100"
    }

    fun message(topic: String, body: String): String? = when {
        topic.isBlank() || body.isBlank() -> "Add a topic and a message"
        topic.trim().length > 120 -> "Keep the topic under 120 characters"
        body.trim().length > 2000 -> "Keep the message under 2000 characters"
        else -> null
    }
}

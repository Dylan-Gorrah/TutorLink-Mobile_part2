package com.snokonoko.tutor_link.domain

import com.snokonoko.tutor_link.data.MessageDto
import com.snokonoko.tutor_link.data.SessionDto
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pay maths, as the demo does it: confirmed × rate, pending × rate, everything × rate. */
data class PaySummary(val approved: Double, val pending: Double, val projected: Double, val confirmedCount: Int)

object PayMath {
    fun summary(sessions: List<SessionDto>, rate: Double): PaySummary {
        val confirmed = sessions.count { it.confirmed }
        val pending = sessions.size - confirmed
        return PaySummary(confirmed * rate, pending * rate, sessions.size * rate, confirmed)
    }

    fun rand(amount: Double): String {
        val whole = amount.toLong()
        val text = if (amount == whole.toDouble()) "%,d".format(Locale.US, whole) else "%,.2f".format(Locale.US, amount)
        return "R" + text.replace(',', ' ')
    }

    /** The statement export: one line per confirmed session. */
    fun csv(sessions: List<SessionDto>, rate: Double, moduleName: (String) -> String): String {
        val rows = sessions.filter { it.confirmed }.map { s ->
            listOf(s.date, moduleName(s.moduleCode), s.studentCount.toString(), rate.toString(), rate.toString(), if (s.paid) "Paid" else "Confirmed")
        }
        return (listOf(listOf("Date", "Module", "Students", "Rate", "Amount", "Status")) + rows)
            .joinToString("\n") { row -> row.joinToString(",") { cell -> if (cell.any { it == ',' || it == '"' }) "\"" + cell.replace("\"", "\"\"") + "\"" else cell } }
    }
}

/** The timeline dot: hollow = read, solid navy = you sent it, gold = unread. */
enum class Dot { Read, Mine, Unread }

object MessageLogic {
    fun dot(m: MessageDto, myId: String, viewerIsTutor: Boolean): Dot = when {
        m.senderId == myId -> Dot.Mine
        viewerIsTutor && !m.readByTutor -> Dot.Unread
        else -> Dot.Read
    }

    /** The sender sees "delivered" until the tutor opens it. */
    fun delivered(m: MessageDto, myId: String) = m.senderId == myId && !m.readByTutor

    fun unreadCount(messages: List<MessageDto>) = messages.count { !it.readByTutor }

    fun parse(iso: String): Instant? = try {
        OffsetDateTime.parse(iso).toInstant()
    } catch (_: Exception) {
        try { Instant.parse(iso) } catch (_: Exception) { null }
    }

    /** "now", "42m", "5h", "Yst", "3d", then "12 Oct" — as in the demo. */
    fun ago(iso: String, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
        val t = parse(iso) ?: return ""
        val d = Duration.between(t, now)
        return when {
            d.toMinutes() < 1 -> "now"
            d.toHours() < 1 -> "${Math.round(d.seconds / 60.0)}m"
            d.toDays() < 1 -> "${Math.round(d.seconds / 3600.0)}h"
            d.toDays() < 2 -> "Yst"
            d.toDays() < 7 -> "${Math.round(d.seconds / 86400.0)}d"
            else -> DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH).format(t.atZone(zone))
        }
    }

    fun fullTime(iso: String, zone: ZoneId = ZoneId.systemDefault()): String {
        val t = parse(iso) ?: return ""
        return DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH).format(t.atZone(zone))
    }
}

package com.snokonoko.tutor_link

import com.snokonoko.tutor_link.data.ApiClient
import com.snokonoko.tutor_link.data.ApiResult
import com.snokonoko.tutor_link.data.LogSessionRequest
import com.snokonoko.tutor_link.data.MessageDto
import com.snokonoko.tutor_link.data.SessionDto
import com.snokonoko.tutor_link.data.TutorRepository
import com.snokonoko.tutor_link.domain.Dot
import com.snokonoko.tutor_link.domain.MessageLogic
import com.snokonoko.tutor_link.domain.PayMath
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class TutorLogicTest {
    private fun session(confirmed: Boolean, paid: Boolean = false, code: String = "PROG1111") =
        SessionDto(id = "s", moduleCode = code, date = "2026-10-01", studentCount = 8, confirmed = confirmed, paid = paid)

    private fun msg(sender: String, read: Boolean) = MessageDto(
        id = "m", tutorId = "tutor_001", senderId = sender, senderRole = "lecturer", senderName = "Dr M. Pretorius",
        topic = "Week 8", body = "Arrays", sentAt = "2026-10-19T10:00:00+00:00", readByTutor = read,
    )

    @Test
    fun payIsConfirmedTimesRate() {
        val s = PayMath.summary(listOf(session(true), session(true, paid = true), session(false)), 150.0)
        assertEquals(300.0, s.approved, 0.0)
        assertEquals(150.0, s.pending, 0.0)
        assertEquals(450.0, s.projected, 0.0)
        assertEquals(2, s.confirmedCount)
    }

    @Test
    fun randFormatting() {
        assertEquals("R1 800", PayMath.rand(1800.0))
        assertEquals("R150", PayMath.rand(150.0))
        assertEquals("R162.50", PayMath.rand(162.5))
    }

    @Test
    fun csvHasOneLinePerConfirmedSession() {
        val csv = PayMath.csv(listOf(session(true), session(false)), 150.0) { "Programming 1" }
        val lines = csv.lines()
        assertEquals("Date,Module,Students,Rate,Amount,Status", lines[0])
        assertEquals(2, lines.size)
        assertEquals("2026-10-01,Programming 1,8,150.0,150.0,Confirmed", lines[1])
    }

    @Test
    fun csvQuotesCommas() {
        val csv = PayMath.csv(listOf(session(true)), 150.0) { "Design, Intro" }
        assertTrue(csv.contains("\"Design, Intro\""))
    }

    @Test
    fun dotsFollowTheDemoRules() {
        assertEquals(Dot.Unread, MessageLogic.dot(msg("lect_001", read = false), "tutor_001", viewerIsTutor = true))
        assertEquals(Dot.Read, MessageLogic.dot(msg("lect_001", read = true), "tutor_001", viewerIsTutor = true))
        assertEquals(Dot.Mine, MessageLogic.dot(msg("lect_001", read = false), "lect_001", viewerIsTutor = false))
        // a lecturer never sees someone else's unread notice as gold
        assertEquals(Dot.Read, MessageLogic.dot(msg("admin_001", read = false), "lect_001", viewerIsTutor = false))
    }

    @Test
    fun deliveredUntilTheTutorOpensIt() {
        assertTrue(MessageLogic.delivered(msg("lect_001", read = false), "lect_001"))
        assertEquals(false, MessageLogic.delivered(msg("lect_001", read = true), "lect_001"))
    }

    @Test
    fun unreadCount() {
        assertEquals(2, MessageLogic.unreadCount(listOf(msg("a", false), msg("b", false), msg("c", true))))
    }

    @Test
    fun agoMatchesTheDemo() {
        val now = Instant.parse("2026-10-19T12:00:00Z")
        assertEquals("now", MessageLogic.ago("2026-10-19T11:59:30Z", now))
        assertEquals("42m", MessageLogic.ago("2026-10-19T11:18:00+00:00", now))
        assertEquals("5h", MessageLogic.ago("2026-10-19T07:00:00+00:00", now))
        assertEquals("Yst", MessageLogic.ago("2026-10-18T06:00:00+00:00", now))
        assertEquals("3d", MessageLogic.ago("2026-10-16T12:00:00+00:00", now))
        assertEquals("1 Oct", MessageLogic.ago("2026-10-01T12:00:00+00:00", now, ZoneOffset.UTC))
        assertEquals("", MessageLogic.ago("not a date", now))
    }

    /* ---------- repository against a mock server ---------- */

    private var last: HttpRequestData? = null

    private fun repo(status: HttpStatusCode, body: String) = TutorRepository(
        ApiClient(HttpClient(MockEngine { req -> last = req; respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }), "https://x/functions/v1/api", "anon") { "t" },
    )

    @Test
    fun scheduleIsParsed() = runTest {
        val json = """{"modules":[],"unread":2,"upcoming":[{"id":"sc1","moduleCode":"PROG1111","day":"Today","time":"14:00 – 15:30",
            "venue":"Lab 2","studentCount":14,"live":true,"marked":false}],"month":{"sessions":4,"confirmed":3,"earned":450},"pending":[]}"""
        val r = repo(HttpStatusCode.OK, json).schedule() as ApiResult.Ok
        assertEquals("Lab 2", r.value.upcoming.single().venue)
        assertEquals(450.0, r.value.month.earned, 0.0)
    }

    @Test
    fun markPostsToTheScheduleRow() = runTest {
        repo(HttpStatusCode.OK, """{"session":{"id":"n","moduleCode":"PROG1111","date":"2026-10-19","studentCount":14,"confirmed":false}}""").mark("sc1")
        assertEquals(HttpMethod.Post, last!!.method)
        assertEquals("/functions/v1/api/tutor/sessions/sc1/mark", last!!.url.encodedPath)
    }

    @Test
    fun alreadyMarkedComesBackAsAMessage() = runTest {
        val r = repo(HttpStatusCode.Conflict, """{"error":"This session is already logged"}""").mark("sc1")
        assertEquals(ApiResult.Err("This session is already logged", 409), r)
    }

    @Test
    fun logSessionSendsTheRightBody() = runTest {
        repo(HttpStatusCode.OK, """{"session":{"id":"n","moduleCode":"PROG1111","date":"2026-10-18","studentCount":5,"confirmed":false}}""")
            .logSession(LogSessionRequest("2026-10-18", "PROG1111", 5))
        assertEquals("""{"date":"2026-10-18","moduleCode":"PROG1111","studentCount":5}""", (last!!.body as TextContent).text)
    }

    @Test
    fun markReadIsAPatch() = runTest {
        val r = repo(HttpStatusCode.OK, """{"ok":true,"unread":1}""").markRead("m1") as ApiResult.Ok
        assertEquals(HttpMethod.Patch, last!!.method)
        assertEquals(1, r.value.unread)
    }

    @Test
    fun payIsParsed() = runTest {
        val r = repo(HttpStatusCode.OK, """{"rate":150,"approved":600,"confirmedCount":4,"pendingAmount":150,"projected":750,"paid":false,
            "lines":[{"id":"a","tutorId":"t","moduleCode":"ISEC6321","date":"2026-09-24","studentCount":12,"confirmed":true,"paid":true,"loggedBy":"tutor","amount":150}]}""").pay() as ApiResult.Ok
        assertEquals(600.0, r.value.approved, 0.0)
        assertEquals(150.0, r.value.lines.single().amount, 0.0)
    }
}

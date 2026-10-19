package com.snokonoko.tutor_link

import com.snokonoko.tutor_link.data.AccountRepository
import com.snokonoko.tutor_link.data.ApiClient
import com.snokonoko.tutor_link.data.ApiResult
import com.snokonoko.tutor_link.data.Errors
import com.snokonoko.tutor_link.data.RegisterLecturerRequest
import com.snokonoko.tutor_link.data.SettingsPatch
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ApiClientTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private var last: HttpRequestData? = null

    private fun repo(status: HttpStatusCode, body: String, token: String? = "user-token"): AccountRepository {
        val engine = MockEngine { req ->
            last = req
            respond(body, status, jsonHeaders)
        }
        return AccountRepository(ApiClient(HttpClient(engine), "https://x.supabase.co/functions/v1/api", "anon-key") { token })
    }

    private val meJson = """
        {"account":{"id":"tutor_001","role":"tutor","name":"Refiloe Matsau","email":"r@tutor.rosebank.ac.za",
        "studentNumber":"ST10312234","approvedAt":"2026-03-15","paid":false,"extra":"ignored"},
        "modules":[{"code":"PROG1111","name":"Programming 1"}],
        "settings":{"haptics":false,"reduceMotion":true,"messagePreviews":true},"unread":2}
    """.trimIndent()

    @Test
    fun meIsParsed() = runTest {
        val r = repo(HttpStatusCode.OK, meJson).me()
        assertTrue(r is ApiResult.Ok)
        val me = (r as ApiResult.Ok).value
        assertEquals("tutor", me.account.role)
        assertEquals(2, me.unread)
        assertEquals(false, me.settings.haptics)
        assertEquals("PROG1111", me.modules.single().code)
    }

    @Test
    fun requestCarriesTheUserTokenAndPath() = runTest {
        repo(HttpStatusCode.OK, meJson).me()
        assertEquals("Bearer user-token", last!!.headers[HttpHeaders.Authorization])
        assertEquals("anon-key", last!!.headers["apikey"])
        assertEquals("/functions/v1/api/me", last!!.url.encodedPath)
    }

    @Test
    fun serverErrorMessageIsShownAsIs() = runTest {
        val r = repo(HttpStatusCode.BadRequest, """{"error":"Pick at least one module"}""").saveModules(emptyList())
        assertEquals(ApiResult.Err("Pick at least one module", 400), r)
    }

    @Test
    fun forbiddenBecomesAMessageNotACrash() = runTest {
        val r = repo(HttpStatusCode.Forbidden, """{"error":"Your role can't do that"}""").me()
        assertEquals(403, (r as ApiResult.Err).status)
        assertEquals("Your role can't do that", r.message)
    }

    @Test
    fun serverCrashWithoutJsonGetsAFriendlyMessage() = runTest {
        val r = repo(HttpStatusCode.InternalServerError, "<html>oops</html>").me()
        assertEquals(ApiResult.Err(Errors.GENERIC, 500), r)
    }

    @Test
    fun unauthorisedGetsTheSessionMessage() = runTest {
        val r = repo(HttpStatusCode.Unauthorized, "").me()
        assertEquals(ApiResult.Err(Errors.forStatus(401), 401), r)
    }

    @Test
    fun noTokenMeansSignInAgainWithoutCallingTheServer() = runTest {
        last = null
        val r = repo(HttpStatusCode.OK, meJson, token = null).me()
        assertEquals(401, (r as ApiResult.Err).status)
        assertEquals(null, last)
    }

    @Test
    fun brokenJsonIsHandled() = runTest {
        val r = repo(HttpStatusCode.OK, "{not json").me()
        assertEquals(ApiResult.Err(Errors.BAD_REPLY, -1), r)
    }

    @Test
    fun noInternetBecomesAMessage() = runTest {
        val engine = MockEngine { throw IOException("offline") }
        val r = AccountRepository(ApiClient(HttpClient(engine), "https://x", "anon") { "t" }).me()
        assertEquals(ApiResult.Err(Errors.NO_INTERNET, 0), r)
    }

    @Test
    fun lecturerSignUpUsesTheAnonKey() = runTest {
        repo(HttpStatusCode.OK, """{"ok":true}""", token = null)
            .registerLecturer(RegisterLecturerRequest("Dr A", "a@rosebank.ac.za", "123456", listOf("PROG1111")))
        assertEquals("Bearer anon-key", last!!.headers[HttpHeaders.Authorization])
        val sent = (last!!.body as TextContent).text
        assertTrue(sent.contains("\"modules\":[\"PROG1111\"]"))
    }

    @Test
    fun settingsPatchOnlySendsWhatChanged() = runTest {
        repo(HttpStatusCode.OK, """{"haptics":false,"reduceMotion":false,"messagePreviews":true}""")
            .saveSettings(SettingsPatch(haptics = false))
        assertEquals("""{"haptics":false}""", (last!!.body as TextContent).text)
    }
}

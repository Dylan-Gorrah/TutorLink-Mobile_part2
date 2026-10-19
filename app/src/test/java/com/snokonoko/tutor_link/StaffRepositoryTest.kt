package com.snokonoko.tutor_link

import com.snokonoko.tutor_link.data.ApiClient
import com.snokonoko.tutor_link.data.ApiResult
import com.snokonoko.tutor_link.data.DecisionRequest
import com.snokonoko.tutor_link.data.PostMessageRequest
import com.snokonoko.tutor_link.data.ProgrammePatch
import com.snokonoko.tutor_link.data.StaffRepository
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
import org.junit.Test

class StaffRepositoryTest {
    private var last: HttpRequestData? = null

    private fun repo(status: HttpStatusCode, body: String) = StaffRepository(
        ApiClient(HttpClient(MockEngine { req -> last = req; respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }), "https://x/functions/v1/api", "anon") { "t" },
    )

    private fun sentBody() = (last!!.body as TextContent).text

    @Test
    fun lecturerOutsideTheirModulesGetsTheServerMessage() = runTest {
        val r = repo(HttpStatusCode.Forbidden, """{"error":"You can only message tutors on the modules you teach"}""")
            .send(PostMessageRequest("tutor_011", "Hi", "Hello"))
        assertEquals(ApiResult.Err("You can only message tutors on the modules you teach", 403), r)
    }

    @Test
    fun approveSendsStatusAndMessage() = runTest {
        repo(HttpStatusCode.OK, """{"application":{"id":"app_005","name":"Naledi Mahlangu","status":"approved"},"tutor":{"id":"tutor_x","name":"Naledi Mahlangu","email":"naledi.m@tutor.rosebank.ac.za"}}""")
            .decide("app_005", DecisionRequest("approved", "Welcome"))
        assertEquals("/functions/v1/api/admin/applications/app_005/decision", last!!.url.encodedPath)
        assertEquals("""{"status":"approved","message":"Welcome"}""", sentBody())
    }

    @Test
    fun approvalReturnsTheNewTutor() = runTest {
        val r = repo(HttpStatusCode.OK, """{"application":{"id":"a","name":"N","status":"approved"},"tutor":{"id":"tutor_x","name":"N","email":"n@tutor.rosebank.ac.za"}}""")
            .decide("a", DecisionRequest("approved", "x")) as ApiResult.Ok
        assertEquals("n@tutor.rosebank.ac.za", r.value.tutor!!.email)
    }

    @Test
    fun rateChangeOnlySendsTheRate() = runTest {
        repo(HttpStatusCode.OK, """{"ratePerSession":160,"minMark":75}""").saveProgramme(ProgrammePatch(ratePerSession = 160.0))
        assertEquals(HttpMethod.Put, last!!.method)
        assertEquals("""{"ratePerSession":160.0}""", sentBody())
    }

    @Test
    fun applicationDetailWithMarksIsParsed() = runTest {
        val r = repo(HttpStatusCode.OK, """{"id":"app_001","name":"Dylan","status":"pending","minMark":75,"flag":"pass",
            "marks":[{"code":"ISEC6321","name":"Information Security","mark":85,"result":"PD","appliedFor":true}]}""").application("app_001") as ApiResult.Ok
        assertEquals(85, r.value.marks.single().mark)
        assertEquals("pass", r.value.flag)
    }

    @Test
    fun applicationFilterGoesInTheQuery() = runTest {
        repo(HttpStatusCode.OK, """{"applications":[]}""").applications("under_review")
        assertEquals("under_review", last!!.url.parameters["status"])
    }
}

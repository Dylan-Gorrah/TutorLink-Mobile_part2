package com.snokonoko.tutor_link.data

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.IOException

/** Every API call ends up as one of these, so screens show a toast instead of crashing. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String, val status: Int) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(f: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Ok -> ApiResult.Ok(f(value))
    is ApiResult.Err -> this
}

object Errors {
    const val NO_INTERNET = "No internet connection. Check your connection and try again."
    const val GENERIC = "Something went wrong. Try again."
    const val BAD_REPLY = "The server sent something unexpected. Try again."

    fun forStatus(status: Int): String = when (status) {
        400 -> "Check what you entered and try again."
        401 -> "Your session has expired. Sign in again."
        403 -> "You don't have access to that."
        404 -> "That couldn't be found."
        409 -> "That's already been done."
        else -> GENERIC
    }
}

/**
 * Talks to our api Edge Function. Requests carry the user's access token; public routes
 * (lecturer sign-up) carry the anon key instead.
 */
class ApiClient(
    private val http: HttpClient,
    private val baseUrl: String,
    private val anonKey: String,
    private val token: suspend () -> String?,
) {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }

    suspend inline fun <reified T> get(path: String): ApiResult<T> =
        send(HttpMethod.Get, path, null, serializer<T>())

    suspend inline fun <reified B, reified T> post(path: String, body: B, public: Boolean = false): ApiResult<T> =
        send(HttpMethod.Post, path, json.encodeToString(serializer<B>(), body), serializer<T>(), public)

    suspend inline fun <reified T> post(path: String): ApiResult<T> =
        send(HttpMethod.Post, path, null, serializer<T>())

    suspend inline fun <reified B, reified T> put(path: String, body: B): ApiResult<T> =
        send(HttpMethod.Put, path, json.encodeToString(serializer<B>(), body), serializer<T>())

    suspend inline fun <reified T> patch(path: String): ApiResult<T> =
        send(HttpMethod.Patch, path, null, serializer<T>())

    suspend fun <T> send(
        method: HttpMethod,
        path: String,
        body: String?,
        reader: DeserializationStrategy<T>,
        public: Boolean = false,
    ): ApiResult<T> {
        return try {
            val bearer = if (public) anonKey else token() ?: return ApiResult.Err(Errors.forStatus(401), 401)
            val res = http.request(baseUrl.trimEnd('/') + path) {
                this.method = method
                header("Authorization", "Bearer $bearer")
                header("apikey", anonKey)
                if (body != null) setBody(TextContent(body, ContentType.Application.Json))
            }
            val text = res.bodyAsText()
            if (res.status.isSuccess()) {
                ApiResult.Ok(json.decodeFromString(reader, text))
            } else {
                ApiResult.Err(errorMessage(text) ?: Errors.forStatus(res.status.value), res.status.value)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            ApiResult.Err(Errors.NO_INTERNET, 0)
        } catch (_: SerializationException) {
            ApiResult.Err(Errors.BAD_REPLY, -1)
        } catch (_: IllegalArgumentException) {
            ApiResult.Err(Errors.BAD_REPLY, -1)
        } catch (_: Exception) {
            ApiResult.Err(Errors.GENERIC, -1)
        }
    }

    private fun errorMessage(text: String): String? = try {
        json.decodeFromString(ApiErrorBody.serializer(), text).error?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

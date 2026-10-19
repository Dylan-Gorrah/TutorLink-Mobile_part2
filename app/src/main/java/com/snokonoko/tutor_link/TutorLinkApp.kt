package com.snokonoko.tutor_link

import android.app.Application
import android.content.Context
import com.snokonoko.tutor_link.data.AccountRepository
import com.snokonoko.tutor_link.data.ApiClient
import com.snokonoko.tutor_link.data.AuthRepository
import com.snokonoko.tutor_link.data.LocalSettings
import com.snokonoko.tutor_link.data.RegisterUploader
import com.snokonoko.tutor_link.data.TutorRepository
import com.snokonoko.tutor_link.ui.components.Haptics
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

class TutorLinkApp : Application() {
    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = Graph(this)
    }
}

/** Everything the screens need, built once. */
class Graph(context: Context) {
    val supabase = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
        install(Auth) {
            // Google SSO comes back through tutorlink://auth-callback
            scheme = "tutorlink"
            host = "auth-callback"
        }
        install(Storage)
    }

    private val http = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 15_000
        }
    }

    val auth = AuthRepository(supabase)
    val api = ApiClient(http, BuildConfig.SUPABASE_URL + "/functions/v1/api", BuildConfig.SUPABASE_ANON_KEY) { auth.token() }
    val account = AccountRepository(api)
    val tutor = TutorRepository(api)
    val uploader = RegisterUploader(supabase)
    val localSettings = LocalSettings(context)
    val haptics = Haptics(context)
}

val Context.graph: Graph get() = (applicationContext as TutorLinkApp).graph

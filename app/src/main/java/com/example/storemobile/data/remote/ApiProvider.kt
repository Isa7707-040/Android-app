package com.example.storemobile.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Builds (and rebuilds) the [ApiService] for whatever base URL the user has
 * configured. The desktop API runs with a self-signed HTTPS certificate on the
 * local network, so for development we trust all certificates. The base URL can
 * be changed at runtime from the login screen, which fixes the most common
 * "can't connect" problem without rebuilding the app.
 */
object ApiProvider {

    /**
     * Sensible default for the Android emulator talking to a PC on the same machine.
     * The installed tray server listens on HTTP port 5050 (see ServerHost: it binds
     * http://0.0.0.0:5050), so the default MUST be http + 5050 — not https/7134.
     * 10.0.2.2 is the emulator's alias for the host PC's localhost.
     */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5050/"

    @Volatile
    private var currentUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var service: ApiService? = null

    fun setBaseUrl(url: String) {
        val normalized = normalize(url)
        if (normalized != currentUrl) {
            currentUrl = normalized
            service = null // force rebuild on next access
        }
    }

    fun baseUrl(): String = currentUrl

    @Synchronized
    fun api(): ApiService {
        return service ?: build(currentUrl).also { service = it }
    }

    private fun build(baseUrl: String): ApiService {
        val client = unsafeClient()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun normalize(raw: String): String {
        var url = raw.trim()
        if (url.isEmpty()) return DEFAULT_BASE_URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Server HTTP'da ishlaydi (5050) — scheme yozilmasa http:// qo'shamiz.
            url = "http://$url"
        }
        if (!url.endsWith("/")) url += "/"
        return url
    }

    private fun unsafeClient(): OkHttpClient {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAll, SecureRandom())
        }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
}
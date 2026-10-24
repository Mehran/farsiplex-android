package com.mehran.farsiplex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error carrying an optional HTTP status so the UI can phrase the failure. */
class HttpException(val status: Int, message: String) : IOException(message)

/**
 * Small OkHttp wrapper for farsiplex.com.
 *
 * The site sits behind Cloudflare and rejects non-browser clients, so every
 * request carries a desktop browser User-Agent plus a same-origin `Referer`.
 */
class Http {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            )
            .header("Accept-Language", "en-US,en;q=0.9,fa;q=0.8")
            .header("Referer", "https://farsiplex.com/")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw HttpException(
                    response.code,
                    if (response.code >= 500) "سرور فریپلکس پاسخ نداد (${response.code})"
                    else "درخواست ناموفق بود (${response.code})"
                )
            }
            body
        }
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}

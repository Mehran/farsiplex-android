package com.mehran.farsiplex.data

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * End-to-end check against the live site: catalog scrape → quality list →
 * signed playback URL → byte range from the CDN.
 *
 * Network tests are opt-in so the normal suite stays hermetic:
 *   ./gradlew :app:testDebugUnitTest -PfarsiplexLive
 */
class LiveSiteTest {

    @Test
    fun catalogAndPlaybackChainWorks() = runBlocking {
        assumeTrue("live tests disabled", java.lang.Boolean.getBoolean("farsiplex.live"))

        val repo = FarsiPlexRepository(Http())

        val home = repo.home()
        assertTrue("home should expose rows", home.rows.isNotEmpty())
        assertTrue(home.rows.first().items.isNotEmpty())

        val movie = home.rows.asSequence()
            .flatMap { it.items.asSequence() }
            .first { !it.isSeries && it.postId != null }

        val details = repo.details(movie.path)
        assertEquals(movie.path, details.card.path)

        val sources = repo.sources(movie.postId!!, false)
        assertTrue("expected at least one stream for ${movie.title}", sources.isNotEmpty())
        assertTrue(sources.all { it.url.startsWith("http") })

        val signed = repo.streamUrl(sources.first(), movie.postId, false)
        assertTrue(signed.startsWith("https://pl."))
        assertTrue(signed.contains("md5="))

        // The signed URL must actually serve video bytes.
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val response = client.newCall(
            Request.Builder()
                .url(signed)
                .header("Range", "bytes=0-1023")
                .header("Referer", "https://farsiplex.com/")
                .build()
        ).execute()
        response.use {
            assertTrue("unexpected status ${it.code}", it.code in 200..299)
            assertEquals("video/mp4", it.header("Content-Type"))
            assertTrue((it.body?.contentLength() ?: 0L) in 1..4096)
        }
    }
}

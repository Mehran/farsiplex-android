package com.mehran.farsiplex.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Catalog + playback access for farsiplex.com.
 *
 * Catalog data is scraped from the server-rendered pages. Playback uses the
 * site's own DooPlay player endpoint, which reports one source per quality
 * (`/wp-json/dooplayer/v2/<post>/<type>/<n>`). Those entries point at a CDN
 * host that only answers with a signed token, so [streamUrl] resolves the real
 * URL through the site's JW Player wrapper page at play time.
 */
class FarsiPlexRepository(private val http: Http) {

    private data class Cached(val at: Long, val html: String)

    private val cache = ConcurrentHashMap<String, Cached>()
    private val pageTtlMs = 5 * 60_000L

    /* ------------------------------ catalog ------------------------------ */

    suspend fun listing(path: String, page: Int = 1): CatalogPage {
        val clean = if (path.startsWith("/")) path else "/$path"
        val url = if (page <= 1) {
            Parser.BASE + clean
        } else {
            Parser.BASE + clean.trimEnd('/') + "/page/$page/"
        }
        return Parser.parseListing(fetch(url))
    }

    suspend fun search(query: String): List<MediaCard> {
        val term = query.trim()
        if (term.length < 2) return emptyList()
        val url = Parser.BASE + "/?s=" + URLEncoder.encode(term, "UTF-8")
        return Parser.parseSearch(fetch(url))
    }

    suspend fun details(path: String): MediaDetails {
        val clean = if (path.startsWith("/")) path else "/$path"
        return Parser.parseDetails(fetch(Parser.BASE + clean), clean)
    }

    /** WordPress post id of any page (movie, series or episode). */
    suspend fun postIdFor(path: String): Int? {
        val clean = if (path.startsWith("/")) path else "/$path"
        return Parser.parsePostId(fetch(Parser.BASE + clean))
    }

    /**
     * The home screen is assembled from the archive pages: the site's landing
     * page is editorial and carries no catalog rows.
     */
    suspend fun home(): HomeContent = coroutineScope {
        val movies = async { listing("/movie/") }
        val series = async { listing("/tvshow/") }
        val action = async { runCatching { listing("/genres/action/") }.getOrNull() }
        val drama = async { runCatching { listing("/genres/drama/") }.getOrNull() }
        val comedy = async { runCatching { listing("/genres/comedy/") }.getOrNull() }
        val thriller = async { runCatching { listing("/genres/thriller/") }.getOrNull() }

        val moviePage = movies.await()
        val seriesPage = series.await()
        val rows = buildList {
            if (moviePage.items.isNotEmpty()) {
                add(CatalogRow("تازه‌ترین فیلم‌ها", moviePage.items))
            }
            if (seriesPage.items.isNotEmpty()) {
                add(CatalogRow("تازه‌ترین سریال‌ها", seriesPage.items))
            }
            action.await()?.items?.takeIf { it.isNotEmpty() }?.let { add(CatalogRow("اکشن", it)) }
            drama.await()?.items?.takeIf { it.isNotEmpty() }?.let { add(CatalogRow("درام", it)) }
            comedy.await()?.items?.takeIf { it.isNotEmpty() }?.let { add(CatalogRow("کمدی", it)) }
            thriller.await()?.items?.takeIf { it.isNotEmpty() }?.let { add(CatalogRow("دلهره‌آور", it)) }
        }
        HomeContent(hero = moviePage.items.firstOrNull(), rows = rows)
    }

    /* ----------------------------- playback ------------------------------ */

    /**
     * Enumerates the title's stream qualities. DooPlay numbers its sources from
     * one; the loop stops once two consecutive indices come back empty.
     */
    suspend fun sources(postId: Int, series: Boolean): List<StreamSource> = coroutineScope {
        val type = if (series) "tv" else "movie"
        // Search up to index 10 to find available qualities; some titles have
        // higher quality indices or dubbed versions beyond the first 4.
        val found = (1..10).map { index ->
            async {
                val url = runCatching { fetchSource(postId, type, index) }.getOrNull()
                if (url == null) null else index to url
            }
        }.awaitAll().filterNotNull().sortedBy { it.first }

        // If no sources found, return empty list early
        val result = if (found.isEmpty()) {
            emptyList<StreamSource>()
        } else {
            val qualities = found.map { Parser.qualityOf(it.second) }
            val duplicateLabels = mutableMapOf<String, Int>()
            found.mapIndexed { position, (index, url) ->
                val quality = qualities[position]
                val base = quality?.let { "${it}p" }
                    ?: if (found.size == 1) "نسخه اصلی" else "نسخه $index"
                val seen = duplicateLabels.getOrDefault(base, 0) + 1
                duplicateLabels[base] = seen
                StreamSource(
                    index = index,
                    label = if (seen == 1) base else "$base · $seen",
                    quality = quality,
                    url = url
                )
            }.sortedWith(
                compareByDescending<StreamSource> { it.quality ?: 0 }.thenBy { it.index }
            )
        }
        result
    }

    /**
     * Turns a catalog source into a URL ExoPlayer can open.
     *
     * The CDN only serves files carrying a per-path token, which the site's
     * JW Player wrapper generates. A cache-busting parameter is required,
     * otherwise a cached response signs whichever file was requested first.
     */
    suspend fun streamUrl(source: StreamSource, postId: Int, series: Boolean): String {
        val type = if (series) "tv" else "movies"
        val encoded = URLEncoder.encode(source.url, "UTF-8")
        val url = Parser.BASE + "/jwplayer/?source=" + encoded +
            "&id=" + postId + "&type=" + type + "&_=" + System.currentTimeMillis()
        val html = http.get(url)
        return Parser.parseSignedFile(html)
            ?: throw IOException("لینک پخش این عنوان در دسترس نیست")
    }

    /** Best available stream for direct playback (hero button, first episode). */
    suspend fun bestStreamUrl(postId: Int, series: Boolean): Pair<String, StreamSource> {
        val list = sources(postId, series)
        val best = list.firstOrNull() ?: throw IOException("لینک پخشی برای این عنوان ثبت نشده است")
        return streamUrl(best, postId, series) to best
    }

    /* ------------------------------- http -------------------------------- */

    private suspend fun fetchSource(postId: Int, type: String, index: Int): String? {
        val url = Parser.BASE + "/wp-json/dooplayer/v2/$postId/$type/$index"
        val body = http.get(url)
        return Parser.parseSourceUrl(body)
    }

    private suspend fun fetch(url: String): String {
        val cached = cache[url]
        if (cached != null && System.currentTimeMillis() - cached.at < pageTtlMs) {
            return cached.html
        }
        val html = http.get(url)
        cache[url] = Cached(System.currentTimeMillis(), html)
        if (cache.size > 64) {
            val cutoff = System.currentTimeMillis() - pageTtlMs
            cache.entries.removeIf { it.value.at < cutoff }
        }
        return html
    }
}

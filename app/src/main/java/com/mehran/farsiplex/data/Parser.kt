package com.mehran.farsiplex.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLDecoder

/**
 * Parses the parts of farsiplex.com the app needs.
 *
 * The site is WordPress with the DooPlay theme: it exposes no catalog JSON, so
 * everything is read from the server-rendered HTML. Search results use a
 * different template than the archive pages, so both shapes are handled.
 */
object Parser {

    const val BASE = "https://farsiplex.com"

    private val YEAR = Regex("^\\d{4}$")
    private val QUALITY = Regex("(?i)(\\d{3,4})p?(?=[._\\- ]|$)")
    private val IMAGE_SIZE = Regex("-\\d+x\\d+(?=\\.[a-zA-Z]{3,4}(\\?|$))")

    private val KNOWN_QUALITIES = setOf(360, 480, 540, 576, 720, 1080, 1440, 2160)

    /** Archive pages (`/movie/`, `/tvshow/`, genre and year pages). */
    fun parseListing(html: String): CatalogPage {
        val doc = Jsoup.parse(html, BASE)
        val items = doc.select("article.item").mapNotNull { el ->
            val link = el.selectFirst(".data h3 a") ?: return@mapNotNull null
            val href = link.absUrl("href").ifBlank { link.attr("href") }
            val type = when {
                el.hasClass("movies") -> MediaType.MOVIE
                el.hasClass("tvshows") -> MediaType.SERIES
                href.contains("/movie/") -> MediaType.MOVIE
                href.contains("/tvshow/") -> MediaType.SERIES
                else -> MediaType.UNKNOWN
            }
            val year = el.selectFirst(".metadata span")?.text()?.trim()
                ?.takeIf { YEAR.matches(it) }
            MediaCard(
                title = link.text().trim(),
                path = pathOf(href),
                type = type,
                year = year,
                posterUrl = el.selectFirst(".poster img")?.attr("src")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::normalizeImage),
                rating = el.selectFirst(".poster .rating")?.text()?.trim()
                    ?.toDoubleOrNull()?.takeIf { it > 0.0 },
                description = el.selectFirst(".texto")?.text()?.trim()
                    ?.takeIf { it.isNotBlank() },
                postId = el.id().removePrefix("post-").toIntOrNull()
            )
        }
        val hasNext = doc.selectFirst(".pagination a.inactive") != null ||
            doc.selectFirst(".resppages a") != null
        return CatalogPage(items, hasNext)
    }

    /** WordPress search results (`/?s=...`), which use their own markup. */
    fun parseSearch(html: String): List<MediaCard> {
        val doc = Jsoup.parse(html, BASE)
        return doc.select("article").mapNotNull { el ->
            val link = el.selectFirst(".details .title a") ?: return@mapNotNull null
            val href = link.absUrl("href").ifBlank { link.attr("href") }
            if (href.isBlank()) return@mapNotNull null
            val type = when {
                href.contains("/movie/") -> MediaType.MOVIE
                href.contains("/tvshow/") -> MediaType.SERIES
                href.contains("/episode/") -> MediaType.EPISODE
                else -> MediaType.UNKNOWN
            }
            MediaCard(
                title = link.text().trim(),
                path = pathOf(href),
                type = type,
                year = el.selectFirst(".meta .year")?.text()?.trim()
                    ?.takeIf { YEAR.matches(it) },
                posterUrl = el.selectFirst(".image img")?.attr("src")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::normalizeImage),
                description = el.selectFirst(".contenido p")?.text()?.trim()
                    ?.takeIf { it.isNotBlank() }
            )
        }
    }

    /** Movie or series detail page. [path] is the site-relative page path. */
    fun parseDetails(html: String, path: String): MediaDetails {
        val doc = Jsoup.parse(html, BASE)
        val type = when {
            path.contains("/tvshow/") -> MediaType.SERIES
            path.contains("/episode/") -> MediaType.EPISODE
            else -> MediaType.MOVIE
        }

        val title = (doc.selectFirst("h1")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty()).trim()
        val poster = doc.selectFirst(".poster img")?.attr("src")
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalizeImage)
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")?.let(::normalizeImage)

        val date = doc.selectFirst(".extra .date")?.text()?.trim()?.takeIf { it.isNotBlank() }
        val year = date?.let { Regex("(19|20)\\d{2}").find(it)?.value }
        val rating = doc.selectFirst(".starstruck[data-rating]")?.attr("data-rating")
            ?.toDoubleOrNull()?.takeIf { it > 0.0 }
            ?: doc.selectFirst(".dt_rating_vgs")?.text()?.trim()?.toDoubleOrNull()
        val votes = doc.selectFirst(".rating-count")?.text()?.trim()?.toIntOrNull()

        val genres = doc.select(".sgeneros a").mapNotNull { it.text().trim().takeIf(String::isNotBlank) }

        val synopsis = doc.select("#info .wp-content p").mapNotNull { p ->
            val text = p.text().trim()
            text.takeIf { it.length >= 40 }
        }.take(2).joinToString("\n\n").ifBlank {
            doc.selectFirst("meta[name=description]")?.attr("content")?.trim().orEmpty()
        }.ifBlank { null }

        val cast = doc.select("#cast .person").mapNotNull { person ->
            val name = person.selectFirst(".name")?.text()?.trim()
                ?: person.selectFirst("meta[itemprop=name]")?.attr("content")?.trim()
            if (name.isNullOrBlank()) return@mapNotNull null
            Person(name, person.selectFirst(".caracter")?.text()?.trim())
        }.distinctBy { it.name }

        val episodes = parseSeasons(doc)

        val postId = postIdFrom(doc)

        val card = MediaCard(
            title = title,
            path = path,
            type = type,
            year = year,
            posterUrl = poster,
            rating = rating,
            description = synopsis?.take(220),
            postId = postId
        )

        return MediaDetails(
            card = card,
            postId = postId,
            originalTitle = doc.selectFirst(".extra .tagline")?.text()?.trim()
                ?.takeIf { it.isNotBlank() && it != title },
            synopsis = synopsis,
            genres = genres,
            cast = cast,
            rating = rating,
            votes = votes,
            date = date,
            country = doc.selectFirst(".extra .country")?.text()?.trim()?.takeIf { it.isNotBlank() },
            backdropUrl = poster,
            seasons = episodes
        )
    }

    /** The WordPress post id of the current page (player form or rating widget). */
    fun parsePostId(html: String): Int? = postIdFrom(Jsoup.parse(html, BASE))

    /**
     * `{"embed_url":"https:\/\/farsiplex.com\/jwplayer\/?source=<cdn url>","type":"mp4"}`
     * → the CDN URL. Null when the title has no source at that index.
     */
    fun parseSourceUrl(json: String): String? {
        val raw = Regex("\"embed_url\"\\s*:\\s*\"([^\"]+)\"").find(json)
            ?.groupValues?.get(1)
            ?.replace("\\/", "/")
            ?: return null
        val query = raw.substringAfter('?', "")
        if (query.isBlank()) return null
        val value = Regex("(?:^|&)source=([^&]+)").find(query)?.groupValues?.get(1) ?: return null
        return URLDecoder.decode(value, "UTF-8").takeIf { it.startsWith("http") }
    }

    /** The signed `file` URL rendered by the site's JW Player wrapper page. */
    fun parseSignedFile(html: String): String? = Regex("\"file\"\\s*:\\s*\"([^\"]+)\"")
        .find(html)
        ?.groupValues?.get(1)
        ?.replace("\\/", "/")
        ?.takeIf { it.startsWith("http") }

    /** Resolution encoded in a stream file name, if the site includes one. */
    fun qualityOf(url: String): Int? {
        val name = url.substringBefore('?').substringAfterLast('/')
        return QUALITY.findAll(name)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .lastOrNull { it in KNOWN_QUALITIES }
    }

    /** `/movie/slug/` from an absolute URL. */
    fun pathOf(href: String): String {
        val raw = href.trim()
        if (raw.startsWith("/")) return raw
        return runCatching { URI(raw).path ?: raw }.getOrDefault(raw)
    }

    /** Drop the WordPress thumbnail suffix so posters render at full size. */
    fun normalizeImage(url: String): String {
        val clean = url.trim()
        if (!clean.startsWith("http")) return clean.replace(IMAGE_SIZE, "")
        return clean.replace(IMAGE_SIZE, "")
    }

    private fun postIdFrom(doc: org.jsoup.nodes.Document): Int? {
        doc.selectFirst("[id^=watch-]")?.id()?.removePrefix("watch-")?.toIntOrNull()
            ?.let { return it }
        doc.selectFirst(".starstruck[data-id]")?.attr("data-id")?.toIntOrNull()
            ?.let { return it }
        doc.selectFirst("meta[property=og:url]")?.attr("content")
            ?.let { Regex("post-(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?.let { return it }
        return null
    }

    private fun parseSeasons(doc: org.jsoup.nodes.Document): List<Season> =
        doc.select("#seasons .se-c").mapIndexedNotNull { seasonIndex, block ->
            val number = block.selectFirst(".se-q .se-t")?.text()?.trim()?.toIntOrNull()
                ?: (seasonIndex + 1)
            val title = block.selectFirst(".se-q .title")?.text()?.trim()
            val episodes = block.select("ul.episodios li").mapNotNull { li ->
                val link: Element = li.selectFirst(".episodiotitle a") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (href.isBlank()) return@mapNotNull null
                val numerando = li.selectFirst(".numerando")?.text()?.trim().orEmpty()
                val episodeNumber = numerando.substringAfterLast('-').trim().toIntOrNull()
                    ?: li.classNames().firstNotNullOfOrNull { name ->
                        Regex("mark-(\\d+)").find(name)?.groupValues?.get(1)?.toIntOrNull()
                    }
                Episode(
                    season = number,
                    number = episodeNumber ?: (episodesCount(block, li) + 1),
                    title = link.text().trim().ifBlank { "قسمت ${episodeNumber ?: 0}" },
                    path = pathOf(href),
                    thumbUrl = li.selectFirst(".imagen img")?.attr("src")
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::normalizeImage),
                    date = li.selectFirst(".date")?.text()?.trim()?.takeIf { it.isNotBlank() }
                )
            }
            if (episodes.isEmpty()) return@mapIndexedNotNull null
            Season(number, title, episodes)
        }

    private fun episodesCount(block: Element, current: Element): Int =
        block.select("ul.episodios li").indexOf(current).coerceAtLeast(0)
}

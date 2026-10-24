package com.mehran.farsiplex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app scrapes farsiplex.com, so these tests run over real pages captured
 * from the live site (app/src/test/resources). They fail if the theme markup
 * changes shape.
 */
class ParserTest {

    private fun read(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader().readText()

    @Test
    fun movieArchiveParsesEveryTile() {
        val page = Parser.parseListing(read("listing_movies.html"))
        assertEquals(30, page.items.size)
        assertTrue(page.hasNext)
        val first = page.items.first()
        assertEquals(MediaType.MOVIE, first.type)
        assertTrue(first.title.isNotBlank())
        assertTrue(first.path.startsWith("/movie/"))
        assertNotNull(first.postId)
        assertNotNull(first.posterUrl)
        // WordPress thumbnail suffixes must be stripped so posters are sharp.
        assertFalse(first.posterUrl!!.contains("-185x278"))
        assertTrue(first.rating == null || first.rating!! > 0.0)
    }

    @Test
    fun seriesArchiveIsTypedAsSeries() {
        val page = Parser.parseListing(read("listing_series.html"))
        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.type == MediaType.SERIES })
        assertTrue(page.items.all { it.path.startsWith("/tvshow/") })
    }

    @Test
    fun searchResultsUseTheSearchTemplate() {
        val results = Parser.parseSearch(read("search.html"))
        assertTrue(results.isNotEmpty())
        val mahoor = results.first { it.title.contains("Mahoor") }
        assertEquals("/movie/mahoor-23c25757/", mahoor.path)
        assertEquals(MediaType.MOVIE, mahoor.type)
        assertTrue(mahoor.posterUrl!!.endsWith("mahoor.jpg"))
    }

    @Test
    fun movieDetailsExposeMetaAndPlayerPostId() {
        val details = Parser.parseDetails(read("details_movie.html"), "/movie/mahoor-23c25757/")
        assertEquals("Mahoor", details.card.title)
        assertEquals(17091, details.postId)
        assertEquals(17091, details.card.postId)
        assertEquals(2.7, details.rating!!, 0.001)
        assertEquals(11, details.votes)
        assertEquals("Iran", details.country)
        assertTrue(details.genres.contains("Drama"))
        assertTrue(details.synopsis!!.contains("romance", ignoreCase = true))
        assertTrue(details.card.posterUrl!!.endsWith("mahoor.jpg"))
        assertFalse(details.isSeries)
    }

    @Test
    fun seriesDetailsExposeSeasonsAndEpisodes() {
        val details = Parser.parseDetails(read("details_series.html"), "/tvshow/bazmandeh-1e1b8820/")
        assertEquals("Bazmandeh", details.card.title)
        assertTrue(details.isSeries)
        assertEquals(14064, details.postId)
        assertEquals(1, details.seasons.size)
        val season = details.seasons.first()
        assertEquals(1, season.number)
        assertEquals(3, season.episodes.size)
        val first = season.episodes.first()
        assertEquals(1, first.number)
        assertTrue(first.path.startsWith("/episode/"))
        assertTrue(first.title.isNotBlank())
        assertNotNull(first.thumbUrl)
    }

    @Test
    fun episodePageKeepsItsOwnPostId() {
        val details = Parser.parseDetails(
            read("details_episode.html"),
            "/episode/bazmandeh-ep01-a532ff67/"
        )
        assertTrue(details.isSeries)
        // The page also carries the parent series' rating id; the player form
        // must win, otherwise episodes would request the series' streams.
        assertEquals(14070, details.postId)
    }

    @Test
    fun playerResponseYieldsTheCdnSource() {
        val json = """
            {"embed_url":"https:\/\/farsiplex.com\/jwplayer\/?source=https%3A%2F%2Fcdn2.farsiland.com%2Fmovies%2F1405%2Fmahoor-720.new.mp4&id=17091&type=mp4","type":"mp4"}
        """.trimIndent()
        assertEquals(
            "https://cdn2.farsiland.com/movies/1405/mahoor-720.new.mp4",
            Parser.parseSourceUrl(json)
        )
        assertNull(Parser.parseSourceUrl("""{"embed_url":null,"type":false}"""))
    }

    @Test
    fun signerPageYieldsTheSignedUrl() {
        val signed = Parser.parseSignedFile(read("signer.html"))
        assertNotNull(signed)
        assertTrue(signed!!.startsWith("https://pl.farsiplex.buzz/"))
        assertTrue(signed.contains("md5="))
    }

    @Test
    fun qualityIsReadFromTheFileName() {
        assertEquals(1080, Parser.qualityOf("https://cdn/movies/1405/High.Copy-1080.mp4"))
        assertEquals(720, Parser.qualityOf("https://cdn/movies/1405/High.Copy-720.mp4"))
        assertEquals(480, Parser.qualityOf("https://cdn/movies/1405/mahoor-480.new.mp4"))
        assertEquals(480, Parser.qualityOf("https://cdn/movies/1405/Bache-Mardom.new.720.480.mp4"))
        assertNull(Parser.qualityOf("https://cdn/movies/1405/Balit.new.mp4"))
        assertNull(Parser.qualityOf("https://cdn/movies/1405/Ghermez-Yavash.mp4"))
    }

    @Test
    fun thumbnailsAreUpgradedToFullSize() {
        assertEquals(
            "https://farsiplex.com/wp-content/uploads/2026/09/mahoor.jpg",
            Parser.normalizeImage("https://farsiplex.com/wp-content/uploads/2026/09/mahoor-214x300.jpg")
        )
        assertEquals(
            "https://farsiplex.com/wp-content/uploads/2026/09/mahoor.jpg",
            Parser.normalizeImage("https://farsiplex.com/wp-content/uploads/2026/09/mahoor-150x150.jpg")
        )
    }
}

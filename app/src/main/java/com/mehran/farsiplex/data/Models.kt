package com.mehran.farsiplex.data

/** What a catalog entry is; drives which player endpoint the app uses. */
enum class MediaType { MOVIE, SERIES, EPISODE, UNKNOWN }

/** One poster tile as scraped from an archive, search or home listing. */
data class MediaCard(
    val title: String,
    val path: String,
    val type: MediaType,
    val year: String? = null,
    val posterUrl: String? = null,
    val rating: Double? = null,
    val description: String? = null,
    /** WordPress post id when the listing exposes it (`article#post-<id>`). */
    val postId: Int? = null
) {
    val isSeries: Boolean get() = type == MediaType.SERIES || type == MediaType.EPISODE
}

data class CatalogPage(val items: List<MediaCard>, val hasNext: Boolean)

data class CatalogRow(val title: String, val items: List<MediaCard>)

data class HomeContent(val hero: MediaCard?, val rows: List<CatalogRow>)

data class Person(val name: String, val role: String? = null)

data class Episode(
    val season: Int,
    val number: Int,
    val title: String,
    val path: String,
    val thumbUrl: String? = null,
    val date: String? = null
)

data class Season(val number: Int, val title: String?, val episodes: List<Episode>)

/**
 * One stream the player endpoint reported. [quality] is the resolution parsed
 * from the file name when the site encodes it (480/720/1080); otherwise null.
 */
data class StreamSource(
    val index: Int,
    val label: String,
    val quality: Int?,
    val url: String
)

data class MediaDetails(
    val card: MediaCard,
    val postId: Int?,
    val originalTitle: String? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<Person> = emptyList(),
    val rating: Double? = null,
    val votes: Int? = null,
    val date: String? = null,
    val country: String? = null,
    val backdropUrl: String? = null,
    val seasons: List<Season> = emptyList()
) {
    val isSeries: Boolean get() = card.type == MediaType.SERIES || card.type == MediaType.EPISODE
}

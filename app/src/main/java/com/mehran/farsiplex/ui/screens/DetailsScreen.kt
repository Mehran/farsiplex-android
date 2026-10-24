package com.mehran.farsiplex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mehran.farsiplex.data.FarsiPlexRepository
import com.mehran.farsiplex.data.MediaDetails
import com.mehran.farsiplex.data.Person
import com.mehran.farsiplex.data.Season
import com.mehran.farsiplex.data.StreamSource
import com.mehran.farsiplex.ui.PlayIntent
import com.mehran.farsiplex.ui.components.ErrorBox
import com.mehran.farsiplex.ui.components.EpisodeCard
import com.mehran.farsiplex.ui.components.FocusButton
import com.mehran.farsiplex.ui.components.LoadingBox
import com.mehran.farsiplex.ui.components.NavTab
import com.mehran.farsiplex.ui.components.PersonAvatar
import com.mehran.farsiplex.ui.components.TopBar
import com.mehran.farsiplex.ui.theme.Bg0
import com.mehran.farsiplex.ui.theme.Brand
import com.mehran.farsiplex.ui.theme.Gold
import com.mehran.farsiplex.ui.theme.Surface1
import com.mehran.farsiplex.ui.theme.Text1
import com.mehran.farsiplex.ui.theme.Text2

private sealed interface DetailsState {
    data object Loading : DetailsState
    data class Ready(val details: MediaDetails) : DetailsState
    data class Error(val message: String) : DetailsState
}

@Composable
fun DetailsScreen(
    repo: FarsiPlexRepository,
    path: String,
    onPlay: (PlayIntent) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSearch: () -> Unit
) {
    var state by remember(path) { mutableStateOf<DetailsState>(DetailsState.Loading) }
    var reload by remember(path) { mutableIntStateOf(0) }
    var sources by remember(path) { mutableStateOf<List<StreamSource>>(emptyList()) }
    var sourcesLoading by remember(path) { mutableStateOf(false) }
    val playFocus = remember { FocusRequester() }

    LaunchedEffect(path, reload) {
        state = DetailsState.Loading
        state = try {
            DetailsState.Ready(repo.details(path))
        } catch (e: Exception) {
            DetailsState.Error(e.message ?: "دریافت اطلاعات ناموفق بود")
        }
    }

    val details = (state as? DetailsState.Ready)?.details
    LaunchedEffect(details?.postId, details?.isSeries) {
        val d = details ?: return@LaunchedEffect
        val id = d.postId ?: return@LaunchedEffect
        if (d.isSeries) return@LaunchedEffect
        sourcesLoading = true
        sources = runCatching { repo.sources(id, false) }.getOrDefault(emptyList())
        sourcesLoading = false
        runCatching { playFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(Bg0)) {
        details?.backdropUrl?.let { backdrop ->
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.22f
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF207080C), Color(0xE607080C), Bg0)
                        )
                    )
            )
        }

        Column(Modifier.fillMaxSize()) {
            TopBar(
                current = when {
                    path.contains("/tvshow/") -> NavTab.Series
                    path.contains("/movie/") -> NavTab.Movies
                    else -> NavTab.Home
                },
                onHome = onHome,
                onMovies = onMovies,
                onSeries = onSeries,
                onSearch = onSearch,
                backLabel = "بازگشت",
                onBack = onBack
            )

            when (val s = state) {
                is DetailsState.Loading -> LoadingBox()
                is DetailsState.Error -> ErrorBox(s.message) { reload++ }
                is DetailsState.Ready -> DetailsContent(
                    details = s.details,
                    sources = sources,
                    sourcesLoading = sourcesLoading,
                    playFocus = playFocus,
                    onPlay = onPlay
                )
            }
        }
    }
}

@Composable
private fun DetailsContent(
    details: MediaDetails,
    sources: List<StreamSource>,
    sourcesLoading: Boolean,
    playFocus: FocusRequester,
    onPlay: (PlayIntent) -> Unit
) {
    val firstEpisode = details.seasons.firstOrNull()?.episodes?.firstOrNull()

    fun playMovie(source: StreamSource?) = onPlay(
        PlayIntent(
            path = details.card.path,
            title = details.card.title,
            series = false,
            postId = details.postId,
            source = source,
            subtitle = source?.label
        )
    )

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier
                    .padding(horizontal = 44.dp)
                    .padding(top = 6.dp)
            ) {
                Box(
                    Modifier
                        .width(200.dp)
                        .height(300.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface1)
                ) {
                    AsyncImage(
                        model = details.card.posterUrl,
                        contentDescription = details.card.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(26.dp))
                Column(Modifier.widthIn(max = 760.dp)) {
                    Text(
                        if (details.isSeries) "سریال" else "فیلم",
                        color = Brand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        details.card.title,
                        color = Text1,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    details.originalTitle?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = Text2, fontSize = 15.sp, maxLines = 1)
                    }
                    Spacer(Modifier.height(10.dp))
                    MetaRow(details)
                    if (details.genres.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            details.genres.take(4).forEach { genre ->
                                Box(
                                    Modifier
                                        .background(Surface1, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(genre, color = Text2, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    if (details.isSeries && firstEpisode == null) {
                        Text(
                            "برای این سریال قسمتی ثبت نشده است",
                            color = Text2,
                            fontSize = 13.sp
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FocusButton(
                                text = if (details.isSeries) "پخش قسمت اول" else "پخش",
                                primary = true,
                                focusRequester = playFocus,
                                onClick = {
                                    if (details.isSeries) {
                                        firstEpisode?.let { episode ->
                                            onPlay(
                                                PlayIntent(
                                                    path = episode.path,
                                                    title = "${details.card.title} · فصل ${episode.season} · قسمت ${episode.number}",
                                                    series = true,
                                                    subtitle = episode.title
                                                )
                                            )
                                        }
                                    } else {
                                        playMovie(sources.firstOrNull())
                                    }
                                }
                            )
                        }
                    }

                    if (!details.isSeries) {
                        Spacer(Modifier.height(14.dp))
                        QualityRow(
                            sources = sources,
                            loading = sourcesLoading,
                            onPick = { playMovie(it) }
                        )
                    }
                }
            }
        }

        details.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
            item {
                Column(Modifier.padding(horizontal = 44.dp).padding(top = 28.dp)) {
                    SectionTitle("خلاصه داستان")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        synopsis,
                        color = Text2,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        modifier = Modifier.widthIn(max = 1100.dp)
                    )
                }
            }
        }

        if (details.cast.isNotEmpty()) {
            item {
                Column(Modifier.padding(top = 26.dp)) {
                    SectionTitle("بازیگران", startPadding = 44.dp)
                    Spacer(Modifier.height(8.dp))
                    CastRow(details.cast)
                }
            }
        }

        if (details.seasons.isNotEmpty()) {
            item {
                SectionTitle(
                    "فصل‌ها و قسمت‌ها",
                    startPadding = 44.dp,
                    modifier = Modifier.padding(top = 26.dp)
                )
            }
            items(details.seasons, key = { it.number }) { season ->
                SeasonBlock(
                    details = details,
                    season = season,
                    onPlay = onPlay
                )
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun MetaRow(details: MediaDetails) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val year = details.card.year ?: details.date?.let { Regex("(19|20)\\d{2}").find(it)?.value }
        year?.let {
            Text(it, color = Text2, fontSize = 14.sp)
            Dot()
        }
        details.country?.let {
            Text(it, color = Text2, fontSize = 14.sp)
            Dot()
        }
        details.rating?.let {
            Text(
                String.format("%.1f", it),
                color = Gold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            details.votes?.let { votes ->
                Text(" ($votes)", color = Text2, fontSize = 13.sp)
            }
            Dot()
        }
        Text(
            if (details.isSeries) "سریال" else "فیلم",
            color = Text2,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun Dot() {
    Text("  ·  ", color = Text2, fontSize = 14.sp)
}

@Composable
private fun QualityRow(
    sources: List<StreamSource>,
    loading: Boolean,
    onPick: (StreamSource) -> Unit
) {
    when {
        loading -> Text("در حال بررسی کیفیت‌های موجود…", color = Text2, fontSize = 12.sp)
        sources.isEmpty() -> Text(
            "برای این عنوان لینک پخشی ثبت نشده است",
            color = Text2,
            fontSize = 12.sp
        )
        else -> {
            Text("کیفیت‌های پخش", color = Text2, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources, key = { it.index }) { source ->
                    FocusButton(text = source.label, onClick = { onPick(source) })
                }
            }
        }
    }
}

@Composable
private fun SeasonBlock(
    details: MediaDetails,
    season: Season,
    onPlay: (PlayIntent) -> Unit
) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(
            season.title ?: "فصل ${season.number}",
            color = Text1,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 44.dp, bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 44.dp)
        ) {
            items(season.episodes, key = { "${it.season}-${it.number}" }) { episode ->
                EpisodeCard(
                    episode = episode,
                    onClick = {
                        onPlay(
                            PlayIntent(
                                path = episode.path,
                                title = "${details.card.title} · فصل ${episode.season} · قسمت ${episode.number}",
                                series = true,
                                subtitle = episode.title
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CastRow(cast: List<Person>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(horizontal = 44.dp)
    ) {
        items(cast.take(20), key = { it.name }) { person ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(96.dp)
            ) {
                PersonAvatar(person.name)
                Spacer(Modifier.height(6.dp))
                Text(
                    person.name,
                    color = Text1,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                person.role?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        color = Text2,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    startPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(start = startPadding)) {
        Box(Modifier.width(4.dp).height(18.dp).background(Brand, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Text1, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

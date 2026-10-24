package com.mehran.farsiplex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mehran.farsiplex.data.HomeContent
import com.mehran.farsiplex.data.MediaCard
import com.mehran.farsiplex.ui.PlayIntent
import com.mehran.farsiplex.ui.components.ErrorBox
import com.mehran.farsiplex.ui.components.FocusButton
import com.mehran.farsiplex.ui.components.LoadingBox
import com.mehran.farsiplex.ui.components.MediaRow
import com.mehran.farsiplex.ui.components.NavTab
import com.mehran.farsiplex.ui.components.TopBar
import com.mehran.farsiplex.ui.theme.Bg0
import com.mehran.farsiplex.ui.theme.Brand
import com.mehran.farsiplex.ui.theme.Gold
import com.mehran.farsiplex.ui.theme.Text1
import com.mehran.farsiplex.ui.theme.Text2

private sealed interface HomeState {
    data object Loading : HomeState
    data class Ready(val content: HomeContent) : HomeState
    data class Error(val message: String) : HomeState
}

@Composable
fun HomeScreen(
    repo: FarsiPlexRepository,
    onOpen: (MediaCard) -> Unit,
    onPlay: (PlayIntent) -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSearch: () -> Unit
) {
    var state by remember { mutableStateOf<HomeState>(HomeState.Loading) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        state = HomeState.Loading
        state = try {
            HomeState.Ready(repo.home())
        } catch (e: Exception) {
            HomeState.Error(e.message ?: "ارتباط با فارسى‌پلکس برقرار نشد")
        }
    }

    Column(Modifier.fillMaxSize().background(Bg0)) {
        TopBar(
            current = NavTab.Home,
            onHome = {},
            onMovies = onMovies,
            onSeries = onSeries,
            onSearch = onSearch
        )

        when (val s = state) {
            is HomeState.Loading -> LoadingBox()
            is HomeState.Error -> ErrorBox(s.message) { reload++ }
            is HomeState.Ready -> LazyColumn(Modifier.fillMaxSize()) {
                item { Hero(s.content.hero, onPlay, onOpen) }
                items(s.content.rows, key = { it.title }) { row ->
                    MediaRow(title = row.title, items = row.items, onOpen = onOpen)
                }
                item { Spacer(Modifier.height(44.dp)) }
            }
        }
    }
}

@Composable
private fun Hero(
    card: MediaCard?,
    onPlay: (PlayIntent) -> Unit,
    onOpen: (MediaCard) -> Unit
) {
    if (card == null) {
        Spacer(Modifier.height(8.dp))
        return
    }
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(card.path) { runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxWidth().height(430.dp)) {
        AsyncImage(
            model = card.posterUrl,
            contentDescription = card.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xD907080C),
                        0.45f to Color(0xB307080C),
                        1f to Bg0
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xE607080C), Color(0x6607080C), Color.Transparent)
                    )
                )
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 44.dp, bottom = 34.dp, end = 44.dp)
                .widthIn(max = 760.dp)
        ) {
            Text(
                if (card.isSeries) "سریال ویژه" else "فیلم ویژه",
                color = Brand,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                card.title,
                color = Text1,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                card.year?.let {
                    Text(it, color = Text2, fontSize = 14.sp)
                    Text("  ·  ", color = Text2, fontSize = 14.sp)
                }
                card.rating?.let {
                    Text(
                        String.format("%.1f", it),
                        color = Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("  ·  ", color = Text2, fontSize = 14.sp)
                }
                Text(
                    if (card.isSeries) "سریال" else "فیلم",
                    color = Text2,
                    fontSize = 14.sp
                )
            }
            card.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    color = Text2,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusButton(
                    text = "پخش",
                    primary = true,
                    focusRequester = playFocus,
                    onClick = {
                        if (card.isSeries) {
                            onOpen(card)
                        } else {
                            onPlay(
                                PlayIntent(
                                    path = card.path,
                                    title = card.title,
                                    series = false,
                                    postId = card.postId
                                )
                            )
                        }
                    }
                )
                FocusButton(text = "جزئیات", onClick = { onOpen(card) })
            }
        }
    }
}

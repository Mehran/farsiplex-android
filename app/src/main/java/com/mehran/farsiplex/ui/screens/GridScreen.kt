package com.mehran.farsiplex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehran.farsiplex.data.FarsiPlexRepository
import com.mehran.farsiplex.data.MediaCard
import com.mehran.farsiplex.ui.components.ErrorBox
import com.mehran.farsiplex.ui.components.LoadingBox
import com.mehran.farsiplex.ui.components.NavTab
import com.mehran.farsiplex.ui.components.PosterCard
import com.mehran.farsiplex.ui.components.TopBar
import com.mehran.farsiplex.ui.theme.Bg0
import com.mehran.farsiplex.ui.theme.Brand
import com.mehran.farsiplex.ui.theme.Text1
import com.mehran.farsiplex.ui.theme.Text2
import kotlinx.coroutines.launch

@Composable
fun GridScreen(
    repo: FarsiPlexRepository,
    title: String,
    path: String,
    tab: NavTab,
    onOpen: (MediaCard) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSearch: () -> Unit
) {
    var items by remember(path) { mutableStateOf<List<MediaCard>>(emptyList()) }
    var page by remember(path) { mutableIntStateOf(0) }
    var hasNext by remember(path) { mutableStateOf(true) }
    var loading by remember(path) { mutableStateOf(false) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var focusGiven by remember(path) { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val firstFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    suspend fun load(nextPage: Int) {
        if (loading) return
        loading = true
        error = null
        try {
            val result = repo.listing(path, nextPage)
            items = if (nextPage <= 1) {
                result.items
            } else {
                val known = items.map { it.path }.toSet()
                items + result.items.filterNot { it.path in known }
            }
            hasNext = result.hasNext
            page = nextPage
        } catch (e: Exception) {
            error = e.message ?: "دریافت فهرست ناموفق بود"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(path) { load(1) }

    // Only the first page grabs focus; later pages must not steal it while the
    // viewer is scrolling deeper into the grid.
    LaunchedEffect(items.size) {
        if (!focusGiven && items.isNotEmpty()) {
            focusGiven = true
            runCatching { firstFocus.requestFocus() }
        }
    }

    // Infinite scroll: pull the next archive page before the grid ends.
    LaunchedEffect(gridState, items.size, hasNext) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (hasNext && !loading && items.isNotEmpty() && last >= items.size - 8) {
                    load(page + 1)
                }
            }
    }

    Column(Modifier.fillMaxSize().background(Bg0)) {
        TopBar(
            current = tab,
            onHome = onHome,
            onMovies = onMovies,
            onSeries = onSeries,
            onSearch = onSearch,
            backLabel = "بازگشت",
            onBack = onBack
        )
        Text(
            title,
            color = Text1,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 44.dp, top = 4.dp, bottom = 12.dp)
        )

        when {
            items.isEmpty() && error != null -> ErrorBox(error!!) { scope.launch { load(1) } }
            items.isEmpty() && loading -> LoadingBox()
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("موردی برای نمایش نیست", color = Text2, fontSize = 16.sp)
            }
            else -> LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(horizontal = 44.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items, key = { _, card -> card.path }) { index, card ->
                    PosterCard(
                        card = card,
                        onClick = { onOpen(card) },
                        autoFocus = if (index == 0) firstFocus else null
                    )
                }
                if (loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier.fillMaxWidth().height(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Brand, strokeWidth = 2.dp)
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

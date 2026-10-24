package com.mehran.farsiplex.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mehran.farsiplex.R
import com.mehran.farsiplex.data.Episode
import com.mehran.farsiplex.data.MediaCard
import com.mehran.farsiplex.ui.theme.Bg1
import com.mehran.farsiplex.ui.theme.Brand
import com.mehran.farsiplex.ui.theme.Gold
import com.mehran.farsiplex.ui.theme.Surface1
import com.mehran.farsiplex.ui.theme.Surface2
import com.mehran.farsiplex.ui.theme.Text1
import com.mehran.farsiplex.ui.theme.Text2
import com.mehran.farsiplex.ui.theme.Warning
import kotlinx.coroutines.launch

/** Destination highlighted in the top navigation. */
enum class NavTab { Home, Movies, Series, Search }

@Composable
fun TopBar(
    current: NavTab,
    onHome: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    backLabel: String? = null,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xF207080C), Color(0x0007080C))))
            .padding(horizontal = 40.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = R.drawable.logo,
            contentDescription = null,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "MEHRAN",
            color = Text1,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            " FARSIPLEX",
            color = Brand,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.width(28.dp))
        NavChip("خانه", current == NavTab.Home, onHome)
        Spacer(Modifier.width(8.dp))
        NavChip("فیلم‌ها", current == NavTab.Movies, onMovies)
        Spacer(Modifier.width(8.dp))
        NavChip("سریال‌ها", current == NavTab.Series, onSeries)
        Spacer(Modifier.width(8.dp))
        NavChip("جستجو", current == NavTab.Search, onSearch)
        if (onBack != null) {
            Spacer(Modifier.width(18.dp))
            NavChip(backLabel ?: "بازگشت ←", false, onBack)
        }
    }
}

@Composable
fun NavChip(text: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                when {
                    focused -> Color.White
                    selected -> Brand
                    else -> Surface1
                },
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    card: MediaCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 168.dp,
    height: Dp = 252.dp,
    showTitle: Boolean = true,
    autoFocus: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Column(
        modifier
            .width(width)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) scope.launch { requester.bringIntoView() }
            }
            .bringIntoViewRequester(requester)
            .then(if (autoFocus != null) Modifier.focusRequester(autoFocus) else Modifier)
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = if (focused) 1.06f else 1f
                scaleY = if (focused) 1.06f else 1f
            }
    ) {
        Box(
            Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface1)
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) Color.White else Color(0x14FFFFFF),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
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
                            0.6f to Color.Transparent,
                            1f to Color(0xD9000000)
                        )
                    )
            )
            card.rating?.let { rating ->
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        String.format("%.1f", rating),
                        color = Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (card.isSeries) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Brand, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("سریال", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            card.year?.let {
                Text(
                    it,
                    color = Text2,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        if (showTitle) {
            Spacer(Modifier.height(6.dp))
            Text(
                card.title,
                color = if (focused) Color.White else Text1,
                fontSize = 13.sp,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EpisodeCard(
    episode: Episode,
    onClick: () -> Unit,
    autoFocus: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .width(210.dp)
            .onFocusChanged { focused = it.isFocused }
            .then(if (autoFocus != null) Modifier.focusRequester(autoFocus) else Modifier)
            .clickable(onClick = onClick)
            .background(if (focused) Color.White else Surface1, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Surface2)
        ) {
            AsyncImage(
                model = episode.thumbUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "فصل ${episode.season} · قسمت ${episode.number}",
            color = if (focused) Color(0xFF33363D) else Text2,
            fontSize = 11.sp
        )
        Text(
            episode.title,
            color = if (focused) Color(0xFF0B0C10) else Text1,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        episode.date?.let {
            Text(
                it,
                color = if (focused) Color(0xFF555A64) else Text2,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MediaRow(
    title: String,
    items: List<MediaCard>,
    onOpen: (MediaCard) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(top = 22.dp)) {
        Text(
            title,
            color = Text1,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 44.dp, bottom = 10.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 44.dp, vertical = 6.dp)
        ) {
            items(items, key = { it.path + it.title }) { card ->
                PosterCard(card = card, onClick = { onOpen(card) })
            }
        }
    }
}

@Composable
fun FocusButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(
                when {
                    focused -> Color.White
                    primary -> Brand
                    else -> Surface2
                },
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 26.dp, vertical = 12.dp)
    ) {
        Text(
            text,
            color = if (focused) Color(0xFF0B0C10) else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LoadingBox(message: String = "در حال بارگذاری…") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Brand)
            Spacer(Modifier.height(14.dp))
            Text(message, color = Text2, fontSize = 15.sp)
        }
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message,
                color = Warning,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
            if (onRetry != null) {
                Spacer(Modifier.height(16.dp))
                FocusButton("تلاش دوباره", onRetry, primary = true)
            }
        }
    }
}

@Composable
fun LoadingOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(Color(0xE607080C)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Brand)
            Spacer(Modifier.height(16.dp))
            Text(text, color = Text1, fontSize = 16.sp)
        }
    }
}

@Composable
fun MessageDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = "باشه"
) {
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { confirmFocus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .width(560.dp)
                .background(Bg1, RoundedCornerShape(16.dp))
                .border(1.dp, Surface2, RoundedCornerShape(16.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Text1, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(message, color = Text2, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            FocusButton(
                confirmText,
                onDismiss,
                primary = true,
                focusRequester = confirmFocus
            )
        }
    }
}

/** Small circular avatar for the cast strip. */
@Composable
fun PersonAvatar(name: String) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Brand.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Text(name.take(1), color = Brand, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

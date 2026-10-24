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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehran.farsiplex.data.FarsiPlexRepository
import com.mehran.farsiplex.data.MediaCard
import com.mehran.farsiplex.ui.components.NavTab
import com.mehran.farsiplex.ui.components.PosterCard
import com.mehran.farsiplex.ui.components.TopBar
import com.mehran.farsiplex.ui.theme.Bg0
import com.mehran.farsiplex.ui.theme.Brand
import com.mehran.farsiplex.ui.theme.Surface1
import com.mehran.farsiplex.ui.theme.Text1
import com.mehran.farsiplex.ui.theme.Text2
import com.mehran.farsiplex.ui.theme.Warning
import kotlinx.coroutines.delay

/**
 * Search over the site's WordPress search page.
 *
 * There is deliberately no custom on-screen keypad: focusing the field raises
 * the device's own Android TV keyboard (the same one the login-free app uses
 * everywhere else), so the results grid keeps its full height.
 */
@Composable
fun SearchScreen(
    repo: FarsiPlexRepository,
    onOpen: (MediaCard) -> Unit,
    onHome: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaCard>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val keyboard = LocalSoftwareKeyboardController.current
    val fieldFocus = remember { FocusRequester() }

    // Raise the system keyboard as soon as the screen opens, like the login
    // flow does on phones. D-pad focus changes below do the same.
    LaunchedEffect(Unit) {
        delay(180)
        runCatching { fieldFocus.requestFocus() }
        keyboard?.show()
    }

    LaunchedEffect(query) {
        val term = query.trim()
        if (term.length < 2) {
            results = emptyList()
            loading = false
            message = if (term.isEmpty()) null else "حداقل ۲ حرف وارد کنید"
            return@LaunchedEffect
        }
        delay(500)
        loading = true
        message = null
        try {
            results = repo.search(term)
            if (results.isEmpty()) message = "نتیجه‌ای یافت نشد"
        } catch (e: Exception) {
            message = e.message ?: "جستجو ناموفق بود"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(Bg0).imePadding()) {
        TopBar(
            current = NavTab.Search,
            onHome = onHome,
            onMovies = onMovies,
            onSeries = onSeries,
            onSearch = {}
        )

        Text(
            "جستجو",
            color = Text1,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 44.dp, top = 26.dp, bottom = 14.dp)
        )

        Row(
            Modifier
                .padding(horizontal = 44.dp)
                .fillMaxWidth()
                .background(Surface1, RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = Brand,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Text1, fontSize = 20.sp),
                cursorBrush = SolidColor(Brand),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboard?.hide() },
                    onGo = { keyboard?.hide() },
                    onDone = { keyboard?.hide() }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(fieldFocus)
                    .onFocusChanged { if (it.isFocused) keyboard?.show() },
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text("نام فیلم یا سریال…", color = Text2, fontSize = 20.sp)
                        }
                        innerTextField()
                    }
                }
            )
            if (loading) {
                CircularProgressIndicator(
                    color = Brand,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        message?.let {
            Text(
                it,
                color = if (it == "نتیجه‌ای یافت نشد") Text2 else Warning,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 44.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(horizontal = 44.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results, key = { it.path }) { card ->
                PosterCard(card = card, onClick = { onOpen(card) })
            }
        }
    }
}

package com.mehran.farsiplex.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mehran.farsiplex.AppContainer
import com.mehran.farsiplex.data.MediaCard
import com.mehran.farsiplex.data.StreamSource
import com.mehran.farsiplex.ui.components.LoadingOverlay
import com.mehran.farsiplex.ui.components.MessageDialog
import com.mehran.farsiplex.ui.components.NavTab
import com.mehran.farsiplex.ui.screens.DetailsScreen
import com.mehran.farsiplex.ui.screens.GridScreen
import com.mehran.farsiplex.ui.screens.HomeScreen
import com.mehran.farsiplex.ui.screens.PlayerScreen
import com.mehran.farsiplex.ui.screens.SearchScreen
import com.mehran.farsiplex.ui.theme.Bg0
import kotlinx.coroutines.launch
import java.io.IOException

/** Every screen in the app. */
sealed interface Screen {
    data object Home : Screen
    data class Browse(val title: String, val path: String, val tab: NavTab) : Screen
    data object Search : Screen
    data class Details(val path: String) : Screen
    data class Player(val url: String, val title: String, val subtitle: String?) : Screen
}

/** What the user asked to watch; the actual URL is resolved by [AppContent]. */
data class PlayIntent(
    val path: String,
    val title: String,
    val series: Boolean,
    val postId: Int? = null,
    val source: StreamSource? = null,
    val subtitle: String? = null
)

private class NavState {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (canGoBack) stack.removeAt(stack.lastIndex)
    }

    fun resetTo(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }
}

@Composable
fun FarsiPlexRoot(container: AppContainer) {
    var splashDone by remember { mutableStateOf(false) }

    if (!splashDone) {
        SplashScreen(onFinished = { splashDone = true })
    } else {
        AppContent(container)
    }
}

@Composable
private fun AppContent(container: AppContainer) {
    val repo = container.repository
    val nav = remember { NavState() }
    val scope = rememberCoroutineScope()

    var resolving by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    fun openDetails(card: MediaCard) {
        nav.push(Screen.Details(card.path))
    }

    fun startPlayback(intent: PlayIntent) {
        if (resolving) return
        scope.launch {
            resolving = true
            playbackError = null
            try {
                val series = intent.series
                val postId = intent.postId
                    ?: repo.postIdFor(intent.path)
                    ?: throw IOException("شناسه این عنوان پیدا نشد")

                val source = intent.source
                    ?: repo.sources(postId, series).firstOrNull()
                    ?: throw IOException(
                        if (series) "برای این قسمت لینک پخشی ثبت نشده است"
                        else "برای این عنوان لینک پخشی ثبت نشده است"
                    )

                val url = repo.streamUrl(source, postId, series)
                nav.push(
                    Screen.Player(
                        url = url,
                        title = intent.title,
                        subtitle = intent.subtitle ?: source.label
                    )
                )
            } catch (e: Exception) {
                playbackError = e.message ?: "خطا در آماده‌سازی پخش"
            } finally {
                resolving = false
            }
        }
    }

    BackHandler(enabled = nav.canGoBack) { nav.pop() }

    Box(Modifier.fillMaxSize().background(Bg0)) {
        when (val screen = nav.current) {
            is Screen.Home -> HomeScreen(
                repo = repo,
                onOpen = { openDetails(it) },
                onPlay = { intent -> startPlayback(intent) },
                onMovies = {
                    nav.resetTo(Screen.Browse("فیلم‌ها", "/movie/", NavTab.Movies))
                },
                onSeries = {
                    nav.resetTo(Screen.Browse("سریال‌ها", "/tvshow/", NavTab.Series))
                },
                onSearch = { nav.resetTo(Screen.Search) }
            )

            is Screen.Browse -> GridScreen(
                repo = repo,
                title = screen.title,
                path = screen.path,
                tab = screen.tab,
                onOpen = { openDetails(it) },
                onBack = { nav.pop() },
                onHome = { nav.resetTo(Screen.Home) },
                onMovies = {
                    nav.resetTo(Screen.Browse("فیلم‌ها", "/movie/", NavTab.Movies))
                },
                onSeries = {
                    nav.resetTo(Screen.Browse("سریال‌ها", "/tvshow/", NavTab.Series))
                },
                onSearch = { nav.resetTo(Screen.Search) }
            )

            is Screen.Search -> SearchScreen(
                repo = repo,
                onOpen = { openDetails(it) },
                onHome = { nav.resetTo(Screen.Home) },
                onMovies = {
                    nav.resetTo(Screen.Browse("فیلم‌ها", "/movie/", NavTab.Movies))
                },
                onSeries = {
                    nav.resetTo(Screen.Browse("سریال‌ها", "/tvshow/", NavTab.Series))
                }
            )

            is Screen.Details -> DetailsScreen(
                repo = repo,
                path = screen.path,
                onPlay = { intent -> startPlayback(intent) },
                onBack = { nav.pop() },
                onHome = { nav.resetTo(Screen.Home) },
                onMovies = {
                    nav.resetTo(Screen.Browse("فیلم‌ها", "/movie/", NavTab.Movies))
                },
                onSeries = {
                    nav.resetTo(Screen.Browse("سریال‌ها", "/tvshow/", NavTab.Series))
                },
                onSearch = { nav.resetTo(Screen.Search) }
            )

            is Screen.Player -> PlayerScreen(
                url = screen.url,
                title = screen.title,
                subtitle = screen.subtitle
            )
        }

        if (resolving) {
            LoadingOverlay("در حال آماده‌سازی پخش…")
        }

        playbackError?.let { message ->
            MessageDialog(
                title = "پخش امکان‌پذیر نیست",
                message = message,
                onDismiss = { playbackError = null }
            )
        }
    }
}

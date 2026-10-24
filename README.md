# Mehran FarsiPlex — Android TV client (unofficial)

A native **Android TV / NetBox** client for [farsiplex.com](https://farsiplex.com/),
written in Kotlin with Jetpack Compose. It reads the site's Persian movie and
series catalogue, shows every stream quality the site exposes, and plays the
video in an integrated ExoPlayer instance.

> **Disclaimer:** unofficial and unaffiliated with FarsiPlex. The app contains no
> media; it only links to streams that the site itself publishes. A working
> internet connection is required, and availability of any title is controlled by
> the website.

---

## Download

| Artifact | Link |
|---|---|
| APK (debug, **armeabi-v7a**) | [`dist/mehran-farsiplex-1.0.0-armeabi-v7a-debug.apk`](dist/mehran-farsiplex-1.0.0-armeabi-v7a-debug.apk) |
| Release | [Releases page](https://github.com/Mehran/farsiplex-android/releases/latest) |

```bash
adb connect <tv-ip>:5555
adb install -r mehran-farsiplex-1.0.0-armeabi-v7a-debug.apk
adb shell am start -n com.mehran.farsiplex/.MainActivity
```

The APK is **32-bit ARM only** (`armeabi-v7a`), built for NetBox / Android TV
boxes. `versionName 1.0.0`, `versionCode 1`. The app appears as
**Mehran FarsiPlex** in the Android TV launcher with a custom banner and icon.

## Features

- **Splash** — animated launch screen with the mascot logo and
  **“Welcome to MehranFarsiPlex”**.
- **Home** — a featured hero with an instant **پخش** (Play) button plus rows for
  the newest movies, newest series and popular genres, all scraped live from the
  site.
- **Browse** — paginated poster grids for movies and series (infinite scroll;
  the grid pulls the next archive page before you reach the end).
- **Search** — the field raises the **device's native Android keyboard** (there is
  no custom on-screen keypad), with live results after a short debounce.
- **Details** — poster, original title, year, country, rating and vote count,
  genres, full synopsis and cast, plus a **quality row** listing every stream the
  site reports (e.g. `1080p`, `720p`, `480p`) and a Play button that starts the
  best available quality immediately.
- **Series** — seasons and episodes are listed with thumbnails and air dates;
  selecting one starts playback of that episode.
- **Player** — ExoPlayer with the stock TV controller, a title/quality overlay
  that fades after a few seconds, clear error messages, and
  `FLAG_KEEP_SCREEN_ON` while playing so the box's screensaver never interrupts
  a film.
- **TV-first UI** — D-pad focus rings, focus-driven smooth scrolling
  (`bringIntoView`), initial focus on the primary action of every screen, and the
  Back key wired to the navigation stack.

## Screens & flow

```
Splash ──▶ Home ──▶ Details ──▶ Player
             │         └── quality ▸ play
             │  └──▶ Browse (movies / series / genres) ──▶ Details
             └──▶ Search ──▶ Details
Series details ──▶ season ▸ episode ──▶ Player
```

## How the site works (reverse-engineered)

`farsiplex.com` is WordPress running the **DooPlay** theme. It publishes no
catalog JSON for movies/series, so the app scrapes the server-rendered HTML:

| Page | Purpose |
|---|---|
| `/movie/`, `/movie/page/N/` | movie archive |
| `/tvshow/`, `/tvshow/page/N/` | series archive |
| `/genres/{slug}/`, `/genres/{slug}/page/N/` | genre archives |
| `/movie/{slug}/` | movie details |
| `/tvshow/{slug}/` | series details with seasons + episodes |
| `/episode/{slug}/` | one episode |
| `/?s={term}` | search results (uses a different template than archives) |

### Streams and qualities

Playback is served by the theme's own player endpoint:

```
GET /wp-json/dooplayer/v2/{postId}/{type}/{index}
```

`type` is `movie` for films and `episodes` for episodes; `index` counts from 1.
A source looks like:

```json
{"embed_url":"https://farsiplex.com/jwplayer/?source=https%3A%2F%2Fcdn2.farsiland.com%2Fmovies%2F1405%2Fmahoor-720.new.mp4&id=17091&type=mp4","type":"mp4"}
```

The real CDN host (`cdn2.farsiland.com`) publishes **no DNS record**, so the URL
in `source` is not playable as-is. The site's JW Player wrapper signs it:

```
GET https://farsiplex.com/jwplayer/?source=<encoded source>&id=<postId>&type=movies&_=<cache-buster>
```

Response contains `var jw = {"file":"https://pl.farsiplex.buzz/...mp4?md5=..."}`.
Requirements found while probing:

- `type` must be **plural** (`movies` / `episodes`) or the `source` is ignored.
- A cache-busting parameter is mandatory: the page is cached and would otherwise
  return the first signed file for that id regardless of the requested quality.
- Signed URLs answer `206 Partial Content`, `Content-Type: video/mp4`,
  `Accept-Ranges: bytes` — verified end-to-end by `LiveSiteTest`.

Qualities are discovered by walking `index` 1..N and are labelled from the file
name (`…-480.mp4`, `…-720.mp4`, `…-1080.mp4`). Some titles only encode the
original file (`Balit.new.mp4`, `Ghermez-Yavash.mp4`); those are shown as
`نسخه اصلی` / `نسخه N`. The quality list is sorted highest first, so **Play**
picks the best one.

### Cloudflare

The site is behind Cloudflare and rejects non-browser clients. Every request in
`Http.kt` sends a desktop browser `User-Agent`, a same-origin `Referer` and
`Accept-Language`.

## Build from source

Requires **JDK 17** and the Android SDK (**platform 35**, build-tools 35.0.0).

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug -Pabi=armeabi-v7a   # -> app-armeabi-v7a-debug.apk
./gradlew :app:assembleDebug                     # -> universal APK (all ABIs)
./gradlew :app:testDebugUnitTest                 # parser tests (10)
```

`local.properties` must point at the SDK (`sdk.dir=...`) or set `ANDROID_HOME`.

## Testing

`app/src/test/java/com/mehran/farsiplex/data/ParserTest.kt` decodes **HTML
captured from the live site** (saved under `app/src/test/resources/`) with the
app's scrapers, so the suite fails if the theme markup changes: archive tiles,
the separate search template, movie/series/episode details, season parsing, the
player JSON, the signer page and quality detection.

An opt-in network test walks the whole chain — home scrape → details →
quality list → signed URL → `206` video bytes from the CDN:

```bash
./gradlew :app:testDebugUnitTest -PfarsiplexLive --tests "*LiveSiteTest*"
```

## Project layout

```
app/src/main/java/com/mehran/farsiplex/
  MainActivity.kt              fullscreen immersive activity
  FarsiPlexApp.kt              Application + manual DI container
  data/Http.kt                 OkHttp client with browser headers
  data/Parser.kt               Jsoup scrapers (catalog, details, player, signer)
  data/FarsiPlexRepository.kt  catalog access, quality enumeration, URL signing
  data/Models.kt               catalog / details / stream models
  ui/App.kt                    navigation + playback launcher
  ui/Splash.kt                 "Welcome to MehranFarsiPlex" launch screen
  ui/screens/                  Home, Grid, Search, Details, Player
  ui/components/Common.kt      top bar, poster/episode cards, rows, buttons
  ui/theme/Theme.kt            brand palette (#0B7EF4 on near-black)
tools/make_brand_assets.py     launcher icon, TV banner and splash logo
app/src/test/                  parser tests over captured HTML
dist/                          built APK
```

## Branding

The launcher icon, Android TV banner and splash logo are generated from the same
geometry:

```bash
python3 tools/make_brand_assets.py
```

## Known limitations

- **Scraping, not an API.** FarsiPlex has no public catalog API, so a theme
  redesign can break the parser (the tests make that obvious quickly).
- **Series episodes are not playable.** Every episode probed during analysis
  (three different series) returned `embed_url: null` from the player endpoint for
  all type variants; the player wrapper then falls back to a file path that
  returns `404`. The app therefore lists seasons/episodes and reports
  «برای این قسمت لینک پخشی ثبت نشده است» when one is selected. Movies below the
  same limitation when they have no sources (`access` titles are filtered the same
  way).
- Some movie entries have no player source at all (e.g. `Balit` and several
  archive-only pages); the details screen says so instead of failing silently.
- The `-216x…`/`-150x150` WordPress thumbnails are upgraded to full posters for
  the UI, so first load downloads larger images.
- The shipped APK is a **debug** build signed with the Android debug key; build a
  release variant with your own keystore for distribution.

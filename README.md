# Glass IPTV — Android IPTV Player

A complete Android Studio project: a dark, glassy (glassmorphism) IPTV player
built with **Kotlin + Jetpack Compose + Material 3 + Media3 ExoPlayer**.

Live TV, Movies and Series sections, channel logos, favourites, recent
watched, search, categories, and an optional XMLTV EPG — all driven by one
M3U playlist hosted on GitHub.

> **This app is a player only. It does not supply any channels.** It plays
> whatever playlist you point it at. Only use a playlist you have the
> right to use, and respect the terms of the services whose streams it
> lists. The bundled sample playlist contains only freely available
> demo videos (Blender Foundation / Google sample streams).

---

## What is inside

| Area | Implementation |
|---|---|
| Playlist source | `AppConfig.PLAYLIST_URL` — GitHub Raw M3U, fetched on start and on refresh |
| M3U parsing | `data/M3uParser.kt` — handles single-line **and** multi-line `#EXTINF` entries (see below) |
| Categories | Dynamic, from `group-title` in the playlist (Family, Movies, Sports, Live Action, Documentary, Drama, Teen, Comedy, Musical, Fantasy, Adventure, Science Fiction, …) |
| Live vs Movies vs Series | Live = HLS (`.m3u8`); Movies = `group-title="Movies"` (+ VOD files); Series = `group-title="Series"` |
| Player | Media3 ExoPlayer — HLS (`.m3u8`) and MP4/MKV, play/pause, seek for VOD, fullscreen (landscape) |
| EPG | Optional XMLTV feed in `AppConfig.EPG_URL`; shows current/next programme, otherwise "No EPG info" |
| Logos | Coil, with a Live-TV icon fallback when a logo is missing |
| Favourites / Recent | DataStore (persisted locally, survives restarts) |
| Design | Dark theme, frosted-glass cards, translucent chips/panels, gradient background, rounded corners |
| Screens | Home (search + categories + recent), Live TV, Movies, Series, Favourites, Detail, Player |

---

## 1. Open in Android Studio

1. Open **Android Studio** (Hedgehog / Iguana or newer, JDK 17).
2. **File → Open** and select the `iptv-app` folder.
3. Let Gradle sync finish (first sync downloads the dependencies — needs internet).
4. Run on an emulator (API 26+) or a real device with the ▶ button.

No `gradlew` script is bundled; Android Studio supplies Gradle when it
opens the project. From a terminal you can also use your own Gradle 8.7+
installation: `gradle :app:assembleDebug`.

Minimum SDK 26, target/compile SDK 34.

## 2. Where to put your GitHub playlist URL

**Only one file to edit:**

`app/src/main/java/com/iptv/glass/AppConfig.kt`

```kotlin
object AppConfig {
    const val PLAYLIST_URL =
        "https://raw.githubusercontent.com/Abidananta/toffee/refs/heads/main/toffee_playlist.m3u"

    const val EPG_URL: String = ""   // optional XMLTV URL, "" = EPG off
}
```

- It **must be a Raw URL**: `https://raw.githubusercontent.com/<user>/<repo>/<branch>/<file>.m3u`
- Do **not** use the `github.com/.../blob/...` page URL — that returns HTML, not M3U, and the app will fall back to the sample playlist.
- **Update without rebuilding:** edit the `.m3u` file on GitHub and the app picks it up on next start / pull-to-refresh (the refresh button in the Home header). No app update is needed.
- If the download fails (offline, wrong URL, GitHub down) the app falls back to the bundled `app/src/main/assets/sample_playlist.m3u` and shows "Offline — showing sample playlist".

### The configured playlist

The default `PLAYLIST_URL` points at the playlist supplied by the user
(`Abidananta/toffee`, file `toffee_playlist.m3u`). At the time of writing it
lists 76 live HLS channels. Its format has two quirks the parser was
built for — see "M3U format notes" below. No stream URLs from that
playlist are copied into this repo; the app downloads it at runtime.

## 3. Build an APK

In Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk` — copy it
to a phone and install it (allow "Install unknown apps" for the file manager
if asked).

Release: **Build → Generate Signed App Bundle / APK**, create/use a
keystore, choose `release`.

### 3b. Build on GitHub (no Android Studio needed)

A GitHub Actions workflow is already included: `.github/workflows/build-apk.yml`

1. GitHub e ekta notun repo banan, tarpor ei `iptv-app` folder er sob file push koren:
   ```bash
   git init
   git add .
   git commit -m "Glass IPTV app"
   git branch -M main
   git remote add origin https://github.com/<apnar-username>/<repo-name>.git
   git push -u origin main
   ```
2. GitHub repo te jan: **Actions tab > Build APK** — push korar sathe sathe build shuru hobe.
3. Build sesh hole oi run er niche **Artifacts > app-debug** theke `app-debug.apk` download koren, phone e install koren.

Note: ei workflow `gradle` command diye build kore, tai `gradlew` file na thakleo cholbe. Java 17 + Android SDK + Gradle 8.7 GitHub nije setup kore ney.

## 4. EPG (TV guide)

1. Get an XMLTV feed whose `<programme channel="…">` values match your
   channels' `tvg-id` values (falling back to the channel name).
2. Put its URL in `AppConfig.EPG_URL`.
3. Leave it as `""` to disable EPG — the app will not make any EPG request
   and every channel shows "No EPG info". A broken EPG feed never breaks
   channel loading or playback.

## 5. M3U format notes (why the parser is written this way)

The configured playlist does **not** use the textbook single-line form.
Verified against the live file:

- Most entries have **no `tvg-id` / `tvg-name`** — only `group-title`
  and (usually) `tvg-logo`. Channel ids are therefore derived from the
  name + stream URL.
- `#EXTINF` is often **split across two lines**:
  ```
  #EXTINF:-1 group-title="Family"
  tvg-logo="https://…/poster.png",Toffee TV
  ```
- Channel names are sometimes **split across two lines** as well
  (`…,WC` / `Match Recap`, `…,Zee` / `Bangla`).
- Stream URLs are HLS (`.m3u8`) and carry a query string
  (`?edge-cache-token=…`) which must be preserved in full.

`M3uParser.parse()` therefore joins an `#EXTINF` entry's continuation
lines until it reaches the stream URL, then extracts the attributes and
the name (text after the first comma outside quotes, whitespace-collapsed).
The standard single-line form in `sample_playlist.m3u` parses identically.
This was verified with a Python mirror of the parser against both forms.

## 6. Project layout

```
iptv-app/
├── README.md
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── sample_playlist.m3u                  # demo playlist (legal sample streams only)
└── app/
    ├── build.gradle.kts                 # Compose BOM, Media3, Coil, OkHttp, DataStore
    ├── src/main/AndroidManifest.xml     # INTERNET permission
    ├── src/main/assets/sample_playlist.m3u
    └── src/main/java/com/iptv/glass/
        ├── AppConfig.kt                 # <<< edit PLAYLIST_URL / EPG_URL here
        ├── GlassIptvApp.kt              # Application + Coil cache
        ├── MainActivity.kt              # NavHost + glass bottom bar (Home/Live/Movies/Series/Favourites)
        ├── model/Channel.kt             # Channel + EpgProgram, isLive/isMovie/isSeries
        ├── data/
        │   ├── M3uParser.kt             # multi-line M3U parser
        │   ├── EpgParser.kt             # XMLTV parser (best-effort)
        │   ├── IptvRepository.kt        # GitHub fetch + fallback to sample
        │   └── FavoritesStore.kt        # DataStore favourites + recent
        ├── viewmodel/MainViewModel.kt   # loading, search, category filter, EPG lookups
        ├── ui/theme/Theme.kt            # dark glass palette, gradients, typography
        ├── ui/components/GlassComponents.kt  # GlassCard, GlassChip, ChannelLogo, ChannelCard
        └── ui/screens/
            ├── HomeScreen.kt            # search, category chips, recent, all channels
            ├── LiveTvScreen.kt
            ├── MoviesScreen.kt          # 2-column grid, detail on tap
            ├── SeriesScreen.kt          # empty state until playlist has a Series group
            ├── FavoritesScreen.kt
            ├── DetailScreen.kt          # logo, EPG now/next, Watch button
            └── PlayerScreen.kt          # Media3 ExoPlayer, fullscreen, favourite, error state
```

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| App shows the sample playlist | Check `AppConfig.PLAYLIST_URL` is a **Raw** URL; check internet; tap Refresh |
| A channel won't play / "Couldn't play this stream" | Live stream links can expire — update the playlist on GitHub, then refresh in the app |
| No EPG info | EPG is off by default (`EPG_URL = ""`) or the feed has no programme for that `tvg-id` |
| Series tab is empty | The current playlist has no `group-title="Series"` group yet — add one to the M3U and refresh |
| Gradle sync fails | Use Android Studio's bundled JDK 17, and check internet access for Maven Central / Google |

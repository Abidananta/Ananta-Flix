# AFLIX — Android IPTV Player

A complete Android Studio project: a dark, glassy IPTV player built with
**Kotlin + Jetpack Compose + Material 3 + Media3 ExoPlayer**, redesigned
in the style of the reference layout the user asked for: dark navy
background (`#0A0E1A` / `#0F1626`), cyan accent (`#00D4FF`), white channel
logos on white tiles, and a 5-tab bottom bar.

Live TV, Movies and Series sections, channel logos, favourites, recent
watched, search, categories, and an optional XMLTV EPG — all driven by one
M3U playlist hosted on GitHub.

> **This app is a player only. It does not supply any channels, and it
> does not copy any other app's brand, name, or social handles.** It plays
> whatever playlist you point it at. Only use a playlist you have the
> right to use, and respect the terms of the services whose streams it
> lists. 
> **This app is a player only. It does not supply any channels.** It plays
> whatever playlist you point it at. Only use a playlist you have the
> right to use, and respect the terms of the services whose streams it
> lists. 
---

## What is inside

| Area | Implementation |
|---|---|
| Playlist source | `AppConfig.PLAYLIST_URL` — GitHub Raw M3U, fetched on start and on refresh |
| M3U parsing | `data/M3uParser.kt` — handles single-line **and** multi-line `#EXTINF` entries (see below) |
| Categories | Dynamic, from `group-title` in the playlist (Family, Movies, Sports, Live Action, Documentary, Drama, Teen, Comedy, Musical, Fantasy, Adventure, Science Fiction, …) |
| Live vs Movies vs Series | Live = HLS (`.m3u8`); Movies = `group-title="Movies"` (+ VOD files); Series = `group-title="Series"` |
| Player | Media3 ExoPlayer — HLS (`.m3u8`) and MP4/MKV, play/pause, seek for VOD, fullscreen (landscape), Picture-in-Picture when enabled, Data Saver caps quality to SD, "SERVER 1" pill |
| EPG | Optional XMLTV feed in `AppConfig.EPG_URL`; shows current/next programme, otherwise "No EPG info" |
| Logos | Coil, white rounded-square logo tiles. Priority: playlist `tvg-logo` → iptv-org database looked up by channel name (see "Channel logos" below) → monogram tile with the channel's initials on a name-based gradient |
| Favourites / Recent | DataStore (persisted locally, survives restarts) |
| Design | Dark navy (`#0A0E1A` / `#0F1626`), cyan accent (`#00D4FF`), dark glass cards, cyan-bordered category chips, 3-column channel tile grid (4 on wide screens) |
| Screens | **MATCHES** (Sports channels from the playlist — no invented scores/fixtures), **CHANNELS** (main tab: search, category chips, Now Playing bar + 3-column tile grid), **CATEGORIES** (category list with counts), **FAVORITES** (tile grid), **MORE** (AFLIX v1.0, Network Stream, Custom Playlists, PiP + Data Saver toggles, Player Decoder), Detail, Player |
| Now Playing | Channels screen shows a "NOW PLAYING CHANNEL" bar with the channel name, EPG line and a "SERVER 1" pill, plus Join Telegram (only when `AppConfig.TELEGRAM_URL` is set), Reload, Share and Fullscreen buttons |

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
- Do **not** use the `github.com/.../blob/...` page URL — that returns HTML, not M3U, and the app will show an empty list with an error.
- **Update without rebuilding:** edit the `.m3u` file on GitHub and the app picks it up on next start / pull-to-refresh (the refresh button in the Home header). No app update is needed.
- If the download fails (offline, wrong URL, GitHub down) the app shows an empty list with the error message. There is no sample playlist fallback.

### The configured playlist

The default `PLAYLIST_URL` points at the playlist supplied by the user
(`Abidananta/toffee`, file `toffee_playlist.m3u`). At the time of writing it
lists 76 live HLS channels. Its format has two quirks the parser was
built for — see "M3U format notes" below. No stream URLs from that
playlist are copied into this repo; the app downloads it at runtime.

## 2b. Channel logos — loaded from the internet by channel name

Channel logos do **not** have to be in the playlist. `data/ChannelLogoResolver.kt`
resolves them in three steps, after the channel list has already loaded
(logo lookup never blocks the list — tiles show the monogram placeholder
until the real logo arrives):

1. **Playlist first** — `tvg-logo` from the M3U entry, when present.
2. **iptv-org database by name** — the public database at
   `https://iptv-org.github.io`:
   - `api/channels.json` — `{"id": "DeeptoTV.bd", "name": "Deepto TV",
     "alt_names": ["দীপ্ত টিভি"], "country": "BD", …}`. This file has
     **no logo field**.
   - `api/logos.json` — `{"channel": "DeeptoTV.bd", "in_use": true,
     "format": "PNG", "url": "https://i.imgur.com/…png"}`.
   - The resolver joins the two by channel id. Names are normalised
     (lowercase, HD/SD/4K stripped, punctuation removed) and matched
     against the name and every `alt_names` entry, so "Deepto TV HD",
     "Deepto TV" and "দীপ্ত টিভি" all find the same logo. When several
     countries share a name, Bangladesh (`.bd`) is preferred, then India.
     PNG logos are preferred over JPEG; SVG is skipped (Coil has no SVG
     decoder by default).
   - The joined name → logo map is cached in memory and in the app cache
     directory, and re-fetched at most once per 24 hours. If iptv-org is
     unreachable, the app simply keeps the monogram tiles — nothing breaks.
3. **Monogram fallback** — when no logo exists anywhere, the tile shows
   the channel's initials (first letters of up to two words, Bengali-aware:
   "Deepto TV" → "DT", "BTV" → "BT") in bold white on a gradient picked
   from the channel name (cyan/blue, pink/orange, teal/green or purple),
   on the same white rounded square as a real logo.

No logo URLs are hard-coded in the app, and no other app's branding is used.

### Telegram button

The "Join Telegram" button on the Channels screen only appears when you
set your own link in `AppConfig.kt`:

```kotlin
const val TELEGRAM_URL: String = "https://t.me/yourchannel"
```

Leave it as `""` and the button stays hidden.

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
   git commit -m "AFLIX app"
   git branch -M main
   git remote add origin https://github.com/<apnar-username>/<repo-name>.git
   git push -u origin main
   ```
2. GitHub repo te jan: **Actions tab > Build APK** — push korar sathe sathe build shuru hobe.
3. Build sesh hole oi run er niche **Artifacts > app-debug** theke `app-debug.apk` download koren, phone e install koren.

Note: ei workflow `gradle` command diye build kore, tai `gradlew` file na thakleo cholbe.

**GitHub build fix — no `setup-android` step:** the workflow deliberately
does NOT use `android-actions/setup-android@v3`. That step failed on
`ubuntu-latest` with `Warning: Failed to find package 'tools'` and
`sdkmanager ... failed with exit code 1`, because the `tools` package no
longer exists. Instead the workflow uses the Android SDK already
preinstalled on the `ubuntu-latest` runner, accepts licences with
`sdkmanager --licenses`, and installs only
`platforms;android-34`, `build-tools;34.0.0` and `platform-tools`.
Do not re-add the `setup-android` step — it will fail the same way.

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
The standard single-line form parses identically.
This was verified with a Python mirror of the parser against both forms.

## 6. Project layout

```
iptv-app/
├── README.md
├── settings.gradle.kts / build.gradle.kts / gradle.properties
└── app/
    ├── build.gradle.kts                 # Compose BOM, Media3, Coil, OkHttp, DataStore
    ├── src/main/AndroidManifest.xml     # INTERNET permission
    └── src/main/java/com/iptv/glass/
        ├── AppConfig.kt                 # <<< edit PLAYLIST_URL / EPG_URL / TELEGRAM_URL here
        ├── GlassIptvApp.kt              # Application + Coil cache
        ├── MainActivity.kt              # NavHost + 5-tab bottom bar (MATCHES/CHANNELS/CATEGORIES/FAVORITES/MORE)
        ├── model/Channel.kt             # Channel + EpgProgram, isLive/isMovie/isSeries
        ├── data/
        │   ├── M3uParser.kt             # multi-line M3U parser
        │   ├── EpgParser.kt             # XMLTV parser (best-effort)
        │   ├── IptvRepository.kt        # GitHub fetch (no sample fallback), custom-URL override
        │   ├── ChannelLogoResolver.kt   # internet logos by channel name (iptv-org channels+logos APIs, 24h cache, monogram fallback)
        │   ├── FavoritesStore.kt        # DataStore favourites + recent
        │   └── SettingsStore.kt         # DataStore PiP / Data Saver / decoder / custom playlist URL
        ├── viewmodel/MainViewModel.kt   # loading, search, category filter, EPG, now-playing, settings, logo resolution
        ├── ui/theme/Theme.kt            # dark navy + cyan palette, gradients, monogram palettes, typography
        ├── ui/components/GlassComponents.kt  # GlassCard, GlassChip, ChannelLogo (white tile + monogram), ChannelTile, ChannelTileGrid, PlayerArea, NowPlayingBar, NexaIcon
        └── ui/screens/
            ├── ChannelsScreen.kt        # main tab: search, chips, Now Playing bar, 3-column tile grid
            ├── HomeScreen.kt            # compatibility wrapper delegating to ChannelsScreen
            ├── CategoriesScreen.kt      # category list with counts, tap to filter Channels
            ├── MatchesScreen.kt         # Sports channels from the playlist (no invented fixtures)
            ├── MoreScreen.kt            # AFLIX v1.0, Network Stream, Custom Playlists, PiP/Data Saver
            ├── LiveTvScreen.kt          # tile grid, category filter
            ├── MoviesScreen.kt          # tile grid, detail on tap
            ├── SeriesScreen.kt          # empty state until playlist has a Series group
            ├── FavoritesScreen.kt       # tile grid
            ├── DetailScreen.kt          # logo, EPG now/next, Watch button
            └── PlayerScreen.kt          # Media3 ExoPlayer, SERVER 1 pill, PiP, Data Saver, fullscreen
```

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| App shows an empty list / error | Check `AppConfig.PLAYLIST_URL` is a **Raw** URL; check internet; tap Refresh |
| A channel won't play / "Couldn't play this stream" | Live stream links can expire — update the playlist on GitHub, then refresh in the app |
| No EPG info | EPG is off by default (`EPG_URL = ""`) or the feed has no programme for that `tvg-id` |
| Series tab is empty | The current playlist has no `group-title="Series"` group yet — add one to the M3U and refresh |
| Matches tab has no channels | The playlist has no `group-title="Sports"` group yet — add one to the M3U and refresh. No scores/fixtures are shown by design |
| "Join Telegram" button missing | `AppConfig.TELEGRAM_URL` is empty by default — put your own Telegram link there to show it |
| A channel shows initials instead of its logo | No logo was found for that name in the playlist or in iptv-org — the monogram tile is the designed fallback. Check the spelling in the M3U, or add a `tvg-logo="…"` to that entry |
| Logos load slowly on first start | The iptv-org database is fetched once (in the background) and cached for 24 hours; later starts reuse the cache |
| GitHub build fails at "Set up Android SDK" | That step was removed — the workflow must NOT use `android-actions/setup-android` (fails with "Failed to find package 'tools'"). It uses the runner's preinstalled SDK instead |
| Gradle sync fails | Use Android Studio's bundled JDK 17, and check internet access for Maven Central / Google |

# APlay: Android IPTV player

Matches, channels, categories, favorites and a player with picture-in-picture.
GitHub builds the APK, so you do not need Android Studio. Works on Android 8.0 and newer.

## Build it on GitHub

1. Create a free account on github.com and press **New repository**. Choose **Private** if you
   do not want others to see your links or signing key. Tick nothing else and create it.
2. Unzip `APlay.zip` on your computer.
3. On the new repository page press **uploading an existing file**. Open the unzipped folder,
   select **everything inside it**, drag it into the page, and press **Commit changes**.
4. Check that `.github/workflows/build.yml` is in the file list. Folders starting with a dot are
   sometimes skipped. If it is missing: **Add file > Create new file**, type
   `.github/workflows/build.yml` as the name, paste the contents of that file, and commit.
5. Open the **Actions** tab. Wait for **Build APK** to get a green tick. If it did not start,
   choose **Build APK > Run workflow**.
6. Open **Releases** on the repository main page, download `APlay.apk` on your phone and install it.
   Android will ask you to allow installs from your browser.

If the build shows a red cross, open it, copy the lines that start with `e:` or
`What went wrong`, and send them to whoever is helping you.

## Player

- Tapping a channel or match plays it in a window at the top of the screen.
- Controls stay hidden. Tap the video to show them.
- The full screen button turns the video sideways. There you also get the equalizer and the
  channel list. Press and hold a channel in that list to move it.
- To show a **Telegram** button, set `TELEGRAM_URL` in `Config.kt`.

## Servers

Beside the category chips on the **Channels** tab there is a **server** button. It lists every
playlist and file you added (links, `.m3u` files, the `playlists` folder). Tap one to show only its
channels, switch it on or off, edit it, or add a new one. The list reloads by itself every few
minutes and when you return to the app, so edits to an online playlist show up without pressing Refresh.

## Add your channels

**Easiest:** use the `playlists` folder. Put links in `playlists/links.txt` or upload `.m3u` files
there. See `playlists/HOW_TO_ADD.md`.

Any of these works:

- Press **+** at the top, paste an M3U link and press **Add**.
- Press **+**, then **Choose a file from phone**.
- In a browser or chat app, **Share** a playlist link and choose **APlay**.
- Open an `.m3u` file with **APlay**.

The app tests a link before saving it. Turn playlists on or off, rename or delete them in
**More > My playlists**.

To give every user the same list, edit `app/src/main/java/com/aplay/tv/Config.kt`:

```kotlin
const val DEFAULT_PLAYLIST_URL = "https://example.com/my.m3u"
const val DEFAULT_MATCHES_URL = "https://example.com/matches.json"
```

Commit the change and GitHub builds a new APK. Until a playlist is added, the app shows demo streams.

## Matches file

Host a file like this (a raw file link from a public GitHub repository works), then paste its
link in **More > Matches source**.

```json
{
  "matches": [
    {
      "league": "Premier League",
      "leagueLogo": "",
      "start": "2026-12-01T21:00:00+06:00",
      "durationMinutes": 120,
      "home": { "name": "Home Team", "logo": "https://example.com/home.png" },
      "away": { "name": "Away Team", "logo": "https://example.com/away.png" },
      "servers": [
        { "name": "Server 1", "url": "https://example.com/live.m3u8" },
        { "name": "Server 2", "url": "https://example.com/live2.m3u8", "referer": "https://example.com/" }
      ]
    }
  ]
}
```

`start` must include the time zone (`+06:00` is Bangladesh). A match is **Live** from `start`
until `durationMinutes` have passed. Optional server fields: `userAgent`, `referer`, `origin`,
`cookie`, `licenseUrl` (Widevine).

## Change the look

| What | File |
|------|------|
| App name | `app/src/main/res/values/strings.xml` and the title in `MainActivity.kt` |
| Colors | `app/src/main/java/com/aplay/tv/ui/Theme.kt` |
| Icon | `app/src/main/res/drawable/ic_launcher_foreground.xml` |

## Notes

- `app/aplay.keystore` signs the APK. Keep it: a new key means users must uninstall first.
  Its password is in the project, so use a private key before publishing the app widely.
- Only use streams you own or have permission to share.

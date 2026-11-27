# Playlist add korar niyom

Ei folder e ja rakhben, app seta nije theke load korbe. Code bodlate hobe na.

## Upay 1: Link diye (sobcheye sohoj)
`links.txt` file e protiti line e ekta kore likhun:

    Bangla | https://example.com/bangla.m3u
    Sports | https://example.com/sports.m3u

Online file er channel bodlale app e Refresh dilei notun list asbe.

## Upay 2: File diye
`.m3u` ba `.m3u8` file ei folder e upload korun, jemon `bangla.m3u`:

    #EXTM3U
    #EXTINF:-1 tvg-logo="https://example.com/logo.png" group-title="Bangla",Channel Name
    https://example.com/live.m3u8

* `group-title` diye category toiri hoy.
* File er nam `_` ba `.` diye shuru korben na.

## Tarpor
**Commit changes** din. GitHub nije notun APK banabe. Actions e green tick holay
Releases theke install korun.

Shudhu sei channel din jegulo chalanor onumoti apnar ache.

## Auto update (APK build korte hobe na)
GitHub e build hole app nije jane playlists folder kothay. App protiti kichu minute pore ar
app e phire ashle GitHub theke folder ta abar pore. Tai `links.txt` ba kono file edit korle
(Commit changes) app e nije notun list ashe. Repository private hole ei auto update kaj kore na,
tokhon notun APK build korte hobe.

* Protiti link ba file app er Servers tab e alada server card hoy.
* Subfolder e file rakhle seta-o server hoy.
* Link er line er shurute `#` thakle seta bad jay. `#` muchte hobe.

## Matches
* `matches.txt` e `Nam | link` likhun (matches.json er link).
* Ba `.json` file ei folder e rakhun.
* Shob source er match ekshathe dekhay. App e More > Matches source e-o link dewa jay.

## Android TV
Ei APK Android TV te-o install kora jay. TV te prothom bar khulle ekta code dekhay.
Phone e APlay te More > Activate TV te oi code likhle TV active hoy, ar phone er playlist
link, matches link ar app color TV te chole jay. (Online service: ntfy.sh, account lage na.)

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

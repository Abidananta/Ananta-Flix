package com.aplay.tv

/**
 * Put your own links here if every installed copy of the app should load them automatically.
 * Leave them empty to show the built-in demo data until the user adds a link in the app.
 */
object Config {
    const val DEFAULT_PLAYLIST_URL = ""
    const val DEFAULT_MATCHES_URL = ""

    /**
     * The GitHub repository that holds the "playlists" folder, written "owner/name".
     * Leave empty: GitHub builds fill it in by themselves. With it the app reads the folder
     * from GitHub each time, so edits show up without building a new APK.
     */
    const val PLAYLIST_REPO = ""
    const val PLAYLIST_BRANCH = ""
    const val PLAYLIST_FOLDER = "playlists"

    /** Your Telegram link. The "Join Telegram" button stays hidden while this is empty. */
    const val TELEGRAM_URL = ""

    /** Social links shown on the More page. Leave blank to show "Link not added yet". */
    const val FACEBOOK_URL = ""
    const val TWITTER_URL = ""
    const val INSTAGRAM_URL = ""

    /** A web link or an e-mail address for "Contact & Feedback". */
    const val CONTACT_URL = ""
}

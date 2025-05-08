package com.silverlyre.project

import com.silverlyre.messages.LibraryMessage
import com.silverlyre.types.*
import io.kvision.core.Col
import io.kvision.core.Color
import io.kvision.html.Div
import io.kvision.state.ObservableList
import io.kvision.state.ObservableValue
import io.kvision.state.observableListOf
import io.kvision.utils.rem

val HEADER_SIZE = 4.rem
val FOOTER_SIZE = 6.rem
val LEFT_COLUMN = 20.rem
val RIGHT_COLUMN = 25.rem

enum class SilverLyreColours(val colour: Color) {
    SELECTED(Color("#401A1D")),
    HOVER_ITEM(Color("#943230")),
    HOVER_TEXT(Color("")),
    TEXT(Color(Col.WHITESMOKE.name)),
    MUTED_TEXT(Color(Col.DARKGRAY.name)),
    MAIN_BG(Color("#0D0D0D")),
    SECONDARY_BG(Color("#401A1D")),
    TERTIARY_BG(Color("#681D20")),
    SEEK(Color("#531c21"))
}

val clientData = mutableMapOf<String, Any>()

var ctrlPressed = false
var shiftPressed = false

val repertoire = ClientRepertoire()

val currentView = ObservableValue(OpenRepertoire.ALBUMS)
val selectedPlaylistCategory: ObservableValue<SharedPlaylistCategory?> = ObservableValue(null)
val selectedPlaylist: ObservableValue<SharedPlaylist?> = ObservableValue(null)
val botsToManage: ObservableList<SharedBot> = observableListOf()
val discordGuilds: ObservableList<DiscordGuild> = observableListOf()
var selectedItems: ObservableList<SharedRepertoireItem> = observableListOf()
var avariceSelected = ObservableValue(true)

var currentSongLength = ObservableValue<Long>(0)
var currentSongPosition = ObservableValue<Long>(0)
var shouldTimerDoStuff = true

fun changeView(repertoire: OpenRepertoire) {
    currentView.setState(repertoire)
}

enum class OpenRepertoire(val prettyName: String) {
    ALBUMS("Albums"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    PLAYLIST("Playlist"),
    UPLOAD("Upload"),
    SETTINGS("Settings"),
    SEARCH("Search")
}

data class ClientRepertoire(
    val songs: ObservableList<SharedSong> = observableListOf(),
    val albums: ObservableList<SharedAlbum> = observableListOf(),
    val artists: ObservableList<SharedArtist> = observableListOf(),
    val playlistCategories: ObservableList<SharedPlaylistCategory> = observableListOf(),
    val playlists: ObservableList<SharedPlaylist> = observableListOf(),
    val playlistSongs: ObservableList<SharedPlaylistSong> = observableListOf()
) {
    fun updateLibrary(message: LibraryMessage) {
        songs.clear()
        albums.clear()
        artists.clear()
        playlistCategories.clear()
        playlists.clear()
        playlistSongs.clear()

        message.songs?.let { songs.addAll(it) }
        message.albums?.let { albums.addAll(it) }
        albums.sortBy { it.name }
        message.artists?.let { artists.addAll(it) }
        artists.sortBy { it.name }
        message.playlistCategories?.let { playlistCategories.addAll(it) }
        message.playlists?.let {playlists.addAll(it)}
        message.playlistSongs?.let {playlistSongs.addAll(it)}
    }

    fun getAlbumFromId(id: String): SharedAlbum {
        return albums.first { it.id == id }
    }
}
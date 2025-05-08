package com.silverlyre.types
import kotlinx.serialization.Serializable

interface SharedRepertoire {
    val id: String
}

@Serializable
sealed interface SharedRepertoireItem : SharedRepertoire {
    var name: String
}

@Serializable
data class SharedAlbum(
    override val id: String,
    override var name: String,
) : SharedRepertoireItem

@Serializable
data class SharedArtist(
    override val id: String,
    override var name: String,
) : SharedRepertoireItem

@Serializable
data class SharedSong(
    override val id: String,
    override var name: String,
    val artistId: String,
    val albumId: String,
    var order: Int,
    val fileName: String,
    val length: Double,
) : SharedRepertoireItem {
    fun compare(repertoire: SharedRepertoire): Boolean {
        if(repertoire is SharedAlbum) {
            return albumId == repertoire.id
        } else if(repertoire is SharedArtist) {
            return artistId == repertoire.id
        }
        return id == repertoire.id
    }
}

@Serializable
sealed interface SharedPlaylistTable : SharedRepertoireItem

@Serializable
data class SharedPlaylistCategory(
    override val id: String,
    override var name: String,
    val configuredBot: Int,
    val order: Int
) : SharedPlaylistTable

@Serializable
data class SharedPlaylist(
    override val id: String,
    val categoryId: String,
    override var name: String,
    val order: Int
) : SharedPlaylistTable

@Serializable
data class SharedPlaylistSong(
    override val id: String,
    val playlistId: String,
    val songId: String,
    val order: Int
) : SharedRepertoire

@Serializable
enum class RepeatType {
    NO,
    SONG,
    QUEUE
}

@Serializable
data class SharedBot(
    val id: Int,
    val name: String,
    val isPlaying: Boolean = true,
    val trackPlaying: Boolean = false,
    var position: Long = 0,
    val repeatEnabled: RepeatType = RepeatType.NO,
    val shuffleEnabled: Boolean = false,
    val currentSong: SharedSong? = null,
    val manualQueue: List<SharedSong>? = null,
    val collectionPosition: Int? = null,
    val collectionQueue: List<SharedSong>? = null,
)

@Serializable
data class SearchForm(
    val searchTerm: String? = null
)

@Serializable
data class DiscordChannel(
    val id: ULong,
    val name: String
)

@Serializable
data class DiscordGuild(
    val botId: Int,
    val id: ULong,
    val name: String,
    val channels: List<DiscordChannel>
)

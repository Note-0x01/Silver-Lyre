package com.silverlyre.messages

import com.silverlyre.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Message

@Serializable
@SerialName("library")
data class LibraryMessage (
    val songs: List<SharedSong>? = null,
    val albums: List<SharedAlbum>? = null,
    val artists: List<SharedArtist>? = null,
    val playlistCategories: List<SharedPlaylistCategory>? = null,
    val playlists: List<SharedPlaylist>? = null,
    val playlistSongs: List<SharedPlaylistSong>? = null
): Message()

enum class CollectionType {
    ALBUM,
    ARTIST,
    PLAYLIST
}

enum class ModificationType {
    ADD,
    REMOVE,
    REORDER,
    CLEAR
}

@Serializable
@SerialName("modifyManualQueue")
data class ModifyManualQueueMessage(
    val bot: SharedBot,
    val modificationType: ModificationType,
    val songsToModify: List<SharedRepertoireItem>,
    val fistPos: Int? = null,
    val secondPos: Int? = null
): Message()

@Serializable
@SerialName("modifyCollectionQueue")
data class ModifyCollectionQueueMessage(
    val bot: SharedBot,
    val modificationType: ModificationType,
    val songsToModify: List<SharedSong>,
    val fistPos: Int? = null,
    val secondPos: Int? = null,
    val playlistId: String? = null
): Message()

@Serializable
@SerialName("retrieveBotInformation")
data class RetrieveBotInformationMessage(
    val bot: SharedBot? = null
): Message()

enum class MusicPlayerControl {
    PLAY_PAUSE,
    BACK,
    FORWARD,
    SHUFFLE,
    REPEAT,
    SEEK
}

@Serializable
@SerialName("controlMusicPlayer")
data class ControlMusicPlayerMessage(
    val bot: SharedBot,
    val musicPlayerControl: MusicPlayerControl,
    val seekPoint: Long? = 0
): Message()

@Serializable
@SerialName("discordGuildInfo")
data class DiscordGuildInfoMessage(
    val guilds: List<DiscordGuild>
): Message()

@Serializable
@SerialName("commandJoinServer")
data class JoinChannelMessage(
    val bot: SharedBot,
    val channelId: ULong
): Message()

@Serializable
@SerialName("renameItemsMessage")
data class RenameItemsMessage(
    val itemsToUpdate: List<SharedRepertoireItem>
): Message()

@Serializable
@SerialName("removeSelectedFromLibrary")
data class RemoveSelectedFromLibraryMessage(
    val itemsToRemove: List<SharedRepertoireItem>
): Message()

// Playlist Messages
@Serializable
sealed class PlaylistMessage: Message()

@Serializable
@SerialName("addPlaylistCategory")
data class AddPlaylistCategoryMessage(
    val name: String,
    val bot: SharedBot
): PlaylistMessage()

@Serializable
@SerialName("addPlaylistToCategory")
data class AddPlaylistMessage(
    val name: String,
    val category: SharedPlaylistCategory
): PlaylistMessage()

@Serializable
@SerialName("deletePlaylistCategory")
data class DeletePlaylistCategoryMessage(
    val playlist: SharedPlaylistCategory
): PlaylistMessage()

@Serializable
@SerialName("deletePlaylist")
data class DeletePlaylistMessage(
    val playlist: SharedPlaylist
): PlaylistMessage()

@Serializable
@SerialName("movePlaylistToCategory")
data class MovePlaylistToCategory(
    val playlist: SharedPlaylist,
    val destinationCategory: SharedPlaylistCategory
): PlaylistMessage()

@Serializable
@SerialName("addItemsToPlaylist")
data class AddItemsToPlaylistMessage(
    val playlist: SharedPlaylist,
    val items: List<SharedRepertoireItem>
): PlaylistMessage()

@Serializable
@SerialName("removeSongsFromPlaylist")
data class RemoveSongsFromPlaylistMessage(
    val playlist: SharedPlaylist,
    val songs: List<SharedSong>
): PlaylistMessage()

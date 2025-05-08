package com.silverlyre.database

import com.silverlyre.messages.*
import com.silverlyre.sendLibraryUpdate
import com.silverlyre.types.SharedAlbum
import com.silverlyre.types.SharedArtist
import com.silverlyre.types.SharedSong
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PlaylistManager(private val databaseClient: DatabaseClient) {
    private val logger: Logger = LoggerFactory.getLogger("PlaylistManager")
    fun processPlaylistMessage(message: PlaylistMessage) {
        runBlocking {
            when(message) {
                is AddPlaylistCategoryMessage -> addPlaylistCategory(message)
                is AddPlaylistMessage -> addPlaylist(message)
                is AddItemsToPlaylistMessage -> addItemsToPlaylist(message)
                is DeletePlaylistCategoryMessage -> TODO()
                is DeletePlaylistMessage -> TODO()
                is MovePlaylistToCategory -> TODO()
                is RemoveSongsFromPlaylistMessage -> removeSongsFromPlaylist(message)
            }
            sendLibraryUpdate(databaseClient)
        }
    }

    private suspend fun addPlaylistCategory(message: AddPlaylistCategoryMessage) {
        databaseClient.repertoireHandler.createPlaylistCategory(message.name, message.bot.id)
    }

    private suspend fun addPlaylist(message: AddPlaylistMessage) {
        databaseClient.repertoireHandler.createPlaylist(message.category, message.name)
    }

    private suspend fun addItemsToPlaylist(message: AddItemsToPlaylistMessage) {
        when(message.items.firstOrNull()) {
            is SharedAlbum -> {
                databaseClient.repertoireHandler.addSongsToPlaylistFromAlbums(message.playlist, message.items)
            }

            is SharedSong -> {
                databaseClient.repertoireHandler.addSongsToPlaylist(message.playlist, message.items)
            }

            is SharedArtist -> {
                databaseClient.repertoireHandler.addSongsToPlaylistFromArtists(message.playlist, message.items)
            }
            else -> {
                logger.info("Unexpected repertoire type")
            }
        }
    }

    private suspend fun removeSongsFromPlaylist(message: RemoveSongsFromPlaylistMessage) {
        databaseClient.repertoireHandler.removeSongsFromPlaylist(message.playlist, message.songs)
    }
}
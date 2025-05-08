package com.silverlyre.database

import com.mongodb.MongoException
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.silverlyre.database.handlers.RepertoireHandler
import com.silverlyre.database.handlers.SongHandler
import com.silverlyre.sendLibraryUpdate
import com.silverlyre.types.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.BsonInt64
import org.bson.Document
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DatabaseClient(
    connectionString: String
): Thread() {
    private val logger: Logger = LoggerFactory.getLogger("DatabaseClient")
    private val client = MongoClient.create(connectionString = connectionString)
    private val database = client.getDatabase("bardkt")

    val repertoireHandler = RepertoireHandler(database)
    val songHandler = SongHandler(database, repertoireHandler)

    override fun run() {
        runBlocking {
            launch {
                testConnect()
            }
        }
    }
    private suspend fun testConnect() {
        return try {
            val command = Document("ping", BsonInt64(1))
            database.runCommand(command)
            logger.info("MongoDB Database connection successful.")
        } catch (me: MongoException) {
            logger.info(me.message)
        }
    }

    //TODO: might be able to replace songHandler's gets with repertoire
    suspend fun getAllOfTypeShared(type: TableName): List<SharedRepertoire> {
        return when (type) {
            TableName.ALBUMS -> repertoireHandler.getAllShared<Album>()
            TableName.ARTISTS -> repertoireHandler.getAllShared<Artist>()
            TableName.SONGS -> songHandler.getSongsShared()
            TableName.PLAYLIST_CATEGORIES -> repertoireHandler.getAllShared<PlaylistCategory>()
            TableName.PLAYLISTS -> repertoireHandler.getAllShared<Playlist>()
            TableName.PLAYLIST_SONGS -> repertoireHandler.getAllShared<PlaylistSong>()
        }
    }

    suspend fun renameItems(itemsToUpdate: List<SharedRepertoireItem>) {
        logger.info(itemsToUpdate.firstOrNull()?.name)
        when(itemsToUpdate.firstOrNull()) {
            is SharedAlbum -> repertoireHandler.updateItems<Album>(itemsToUpdate)
            is SharedArtist -> repertoireHandler.updateItems<Artist>(itemsToUpdate)
            is SharedPlaylistTable -> {
                when(itemsToUpdate.firstOrNull()) {
                    is SharedPlaylistCategory -> repertoireHandler.updateItems<PlaylistCategory>(itemsToUpdate)
                    is SharedPlaylist -> repertoireHandler.updateItems<Playlist>(itemsToUpdate)
                    else -> logger.info("Shouldn't reach here!")
                }
            }
            is SharedSong -> repertoireHandler.updateItems<Song>(itemsToUpdate)
            null -> TODO()
        }
        sendLibraryUpdate(this)
    }

    suspend fun removeItemsFromLibrary(itemsToRemove: List<SharedRepertoireItem>) {
        when(itemsToRemove.firstOrNull()) {
            is SharedAlbum -> {
                val albumIds = itemsToRemove.map { ObjectId(it.id) }
                val songsInSelectedAlbums = repertoireHandler.getAllSongsInAlbumsSharedAsSongsShared(albumIds)

                repertoireHandler.deleteSongsInList(songsInSelectedAlbums)
            }
            is SharedArtist -> {
                val artistIds = itemsToRemove.map { ObjectId(it.id) }
                val songsInSelectedAlbums = repertoireHandler.getAllSongsInArtistsSharedAsSongsShared(artistIds)

                repertoireHandler.deleteSongsInList(songsInSelectedAlbums)
            }
            is SharedPlaylistTable -> {
                when(itemsToRemove.firstOrNull()) {
                    is SharedPlaylistCategory -> repertoireHandler.deletePlaylistCategory(itemsToRemove.first()
                            as SharedPlaylistCategory)
                    is SharedPlaylist -> repertoireHandler.deletePlaylist(itemsToRemove.first() as SharedPlaylist)
                    else -> logger.info("Shouldn't reach here!")
                }
            }
            is SharedSong -> {
                @Suppress("UNCHECKED_CAST")
                repertoireHandler.deleteSongsInList(itemsToRemove as List<SharedSong>)
            }
            null -> logger.info("Removing items from the library received a null.")
        }
        sendLibraryUpdate(this)
    }
}
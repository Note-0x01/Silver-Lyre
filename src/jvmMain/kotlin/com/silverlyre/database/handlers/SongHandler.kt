package com.silverlyre.database.handlers

import com.silverlyre.types.SharedRepertoire
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.silverlyre.database.*
import com.silverlyre.sendLibraryUpdate
import com.silverlyre.types.SharedSong
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SongHandler(
    private val database: MongoDatabase,
    private val repertoireHandler: RepertoireHandler
) {
    private val logger: Logger = LoggerFactory.getLogger("SongHandler")

    private fun cleanSongName(name: String, fileName: String): String {
        return if(name.trim() == "") fileName.substring(0, fileName.indexOf(".")).trim() else name.trim()
    }

    suspend fun addSong(name: String, artistName: String, albumName: String, fileName: String, length: Double): Song? {
        val cleanArtistName = repertoireHandler.cleanName(artistName)
        val cleanAlbumName = repertoireHandler.cleanName(albumName)
        val cleanSongName = cleanSongName(name, fileName)

        val albumId: ObjectId = repertoireHandler.getOrCreate<Album>(cleanAlbumName).id
        val artistId: ObjectId = repertoireHandler.getOrCreate<Artist>(cleanArtistName).id
        val order: Int = getLastOrderInSongs(albumId) + 1

        logger.info("Adding new song: $cleanSongName in $cleanAlbumName by $cleanArtistName")
        return if(!repertoireHandler.isSongInRepertoire(albumId, artistId, fileName)) {
            val song = Song(
                ObjectId(),
                cleanSongName,
                artistId,
                albumId,
                order,
                fileName,
                length
            )
            database.getCollection<Song>(TableName.SONGS.tableName).insertOne(song)
            song
        } else {
            null
        }
    }

    private suspend fun getLastOrderInSongs(albumId: ObjectId): Int {
        return try {
            database.getCollection<Song>(TableName.SONGS.tableName)
                .find(Filters.eq("albumId", albumId)).limit(1).sort(Sorts.descending(Song::order.name)).last().order
        } catch (_: NoSuchElementException) {
            -1
        }
    }

    suspend fun getSongsShared(): List<SharedRepertoire> {
        val songs = database.getCollection<Song>(TableName.SONGS.tableName).find().map {
            it.convertToShared() }.toList()
        return songs
    }

    suspend fun getAllSongsInAlbumShared(albumId: String): List<SharedSong> {
        val songs = database.getCollection<Song>(TableName.SONGS.tableName)
            .find(Filters.eq("albumId", ObjectId(albumId)))
            .sort(Sorts.ascending(Song::order.name)).map {
            it.convertToShared() as SharedSong }.toList()

        return songs
    }

    suspend fun getAllSongsInArtistShared(artistId: String): List<SharedSong> {
        val songs = database.getCollection<Song>(TableName.SONGS.tableName)
            .find(Filters.eq("artistId", ObjectId(artistId)))
            .sort(Sorts.ascending(Song::order.name)).map {
                it.convertToShared() as SharedSong }.toList()

        return songs
    }

    suspend fun getAllSongsInPlaylistShared(playlistId: String): List<SharedSong> {
        val songIdsInOrder = database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName)
            .find(Filters.eq("playlistId", ObjectId(playlistId)))
            .sort(Sorts.ascending(PlaylistSong::order.name)).map { it.songId }.toList()

        val order = (songIdsInOrder.withIndex().associate { (index, it) -> it.toString() to index })

        val songs = database.getCollection<Song>(TableName.SONGS.tableName)
            .find(Filters.`in`("_id", songIdsInOrder)).map {
                it.convertToShared() as SharedSong }.toList().toMutableList()
            .map { it.order = order[it.id]!!; it }

        return songs.sortedBy { it.order }
    }

}
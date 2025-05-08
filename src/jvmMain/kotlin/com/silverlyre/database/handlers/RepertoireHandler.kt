package com.silverlyre.database.handlers

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.silverlyre.database.*
import com.silverlyre.musicRootPath
import com.silverlyre.types.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class RepertoireHandler(
    private var database: MongoDatabase
) {
    fun cleanName(name: String): String {
        return if(name.trim() == "") "Other" else name.trim()
    }

    private val logger: Logger = LoggerFactory.getLogger("RepertoireHandler")
    internal suspend inline fun <reified T : Repertoire> getOrCreate(name: String): T {
        val collectionName = T::class.simpleName!!
        val processedName = cleanName(name)
        return try {
            database.getCollection<T>(collectionName).find(Filters.eq("name", processedName)).limit(1).first()
        } catch (_: NoSuchElementException) {
            logger.info("Adding new $collectionName: $processedName to database.")
            database.getCollection<T>(collectionName).insertOne(T::class.constructors.last().call(ObjectId(), processedName))
            database.getCollection<T>(collectionName).find(Filters.eq("name", processedName)).limit(1).first()
        }
    }
    internal suspend inline fun <reified T : Repertoire> getAll(): String {
        val all = database.getCollection<T>(T::class.simpleName.toString()).find().map {
            Json.encodeToJsonElement(it)
        }.toList()
        return Json.encodeToString(all)
    }

    internal suspend inline fun <reified T : Repertoire> getAllShared(): List<SharedRepertoire> {
        val all = database.getCollection<T>(T::class.simpleName.toString()).find().map {
            it.convertToShared()
        }.toList()
        return all
    }

    internal suspend inline fun <reified T : Repertoire> updateItems(
        items: List<SharedRepertoireItem>
    ) {
        items.forEach {
            database.getCollection<T>(T::class.simpleName.toString()).updateOne(
                Filters.eq("_id", ObjectId(it.id)),
                Updates.set("name", it.name)
            )
        }
    }

    internal suspend inline fun getAlbumFromId(id: String): SharedRepertoire {
        val album = database.getCollection<Album>(TableName.ALBUMS.tableName).find<Album>(Filters.eq("_id", ObjectId(id))).limit(1).first()
        return album.convertToShared()
    }

    internal suspend inline fun createPlaylistCategory(name: String, botIndex: Int) {
        val lastOrder = try {
            database.getCollection<PlaylistCategory>(TableName.PLAYLIST_CATEGORIES.tableName).find<PlaylistCategory>().sort(
                Sorts.ascending(PlaylistCategory::order.name)).last().order
        } catch (_: NoSuchElementException) {
            -1
        }

        database.getCollection<PlaylistCategory>(TableName.PLAYLIST_CATEGORIES.tableName).insertOne(PlaylistCategory(ObjectId(), name, botIndex, lastOrder+1))
    }

    internal suspend inline fun createPlaylist(playlistCategory: SharedPlaylistCategory, name: String) {
        val lastOrder = try {
            database.getCollection<Playlist>(TableName.PLAYLISTS.tableName).find<Playlist>().sort(
                Sorts.ascending(Playlist::order.name)).last().order
        } catch (_: NoSuchElementException) {
            -1
        }

        database.getCollection<Playlist>(TableName.PLAYLISTS.tableName).insertOne(Playlist(ObjectId(), ObjectId(playlistCategory.id), name, lastOrder+1))
    }

    private suspend fun getLastOrderInPlaylistSongs(playlistId: String): Int {
        return try {
            database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName)
                .find<PlaylistSong>(Filters.eq("playlistId", ObjectId(playlistId))).sort(Sorts.ascending("order")).last().order

        } catch (_: NoSuchElementException) {
            -1
        }
    }

    suspend fun getAllSongsInAlbumsShared(albums: List<ObjectId>): List<ObjectId> {
        val songs = database.getCollection<Song>(TableName.SONGS.tableName)
            .find(Filters.`in`("albumId", albums))
            .sort(Sorts.ascending(Song::order.name)).map {
                it.id }.toList()

        return songs
    }

    suspend fun getAllSongsInAlbumsSharedAsSongsShared(albums: List<ObjectId>): List<SharedSong> {
        return database.getCollection<Song>(TableName.SONGS.tableName)
            .find(Filters.`in`("albumId", albums))
            .sort(Sorts.ascending(Song::order.name)).map { it.convertToShared() as SharedSong }.toList()
    }

    suspend fun getAllSongsInArtistsShared(artists: List<ObjectId>): List<ObjectId> {
        val songs = database.getCollection<Song>(TableName.SONGS.tableName)
            .find(Filters.`in`("artistId", artists))
            .sort(Sorts.ascending(Song::order.name)).map {
                it.id }.toList()

        return songs
    }

    suspend fun getAllSongsInArtistsSharedAsSongsShared(artists: List<ObjectId>): List<SharedSong> {
        return database.getCollection<Song>(TableName.SONGS.tableName)
            .find(Filters.`in`("artistId", artists))
            .sort(Sorts.ascending(Song::order.name)).map { it.convertToShared() as SharedSong }.toList()
    }

    internal suspend inline fun addSongsToPlaylistFromAlbums(playlist: SharedPlaylist, albums: List<SharedRepertoireItem>) {
        val albumIds = albums.map { ObjectId(it.id) }
        val songIds = getAllSongsInAlbumsShared(albumIds)
        for (songId in songIds) {
            val lastOrder = getLastOrderInPlaylistSongs(playlist.id) + 1
            database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).insertOne(PlaylistSong(ObjectId(),
                ObjectId(playlist.id), songId, lastOrder))
        }
    }

    internal suspend inline fun addSongsToPlaylistFromArtists(playlist: SharedPlaylist, artists: List<SharedRepertoireItem>) {
        val artistIds = artists.map { ObjectId(it.id) }
        val songIds = getAllSongsInArtistsShared(artistIds)
        for (songId in songIds) {
            val lastOrder = getLastOrderInPlaylistSongs(playlist.id) + 1
            database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).insertOne(PlaylistSong(ObjectId(),
                ObjectId(playlist.id), songId, lastOrder))
        }
    }

    internal suspend inline fun addSongsToPlaylist(playlist: SharedPlaylist, songs: List<SharedRepertoireItem>) {
        for(songId in songs.map {ObjectId(it.id)}) {
            val lastOrder = getLastOrderInPlaylistSongs(playlist.id) + 1
            database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).insertOne(PlaylistSong(ObjectId(),
                ObjectId(playlist.id), songId, lastOrder))
        }
    }

    internal suspend fun removeSongsFromPlaylist(playlist: SharedPlaylist, songs: List<SharedRepertoireItem>) {
        val songIds = songs.map { ObjectId(it.id) }
        val filter = Filters.and(
            Filters.eq("playlistId", ObjectId(playlist.id)),
            Filters.`in`("songId", songIds )
        )
        database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).deleteMany(filter)

        var order = 0;
        database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).find(Filters.eq("playlistId",
            ObjectId(playlist.id))).sort(Sorts.ascending("order")).toList().forEach {
            database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).updateOne(
                Filters.eq("_id", it.id),
                Updates.set("order", order++)
            )
        }
    }

    internal suspend fun deleteSongsInList(songs: List<SharedSong>) {
        val albumIds = songs.map { ObjectId(it.albumId) }.distinct()
        val artistIds = songs.map { ObjectId(it.artistId) }.distinct()
        val songsToRemove = songs.map { ObjectId(it.id) }

        val songsPathsToDelete = database.getCollection<Song>(TableName.SONGS.tableName)
            .find<Song>(Filters.`in`("_id", songsToRemove)).toList()
            .map { "$musicRootPath/${it.albumId}/${it.fileName}" }

        songsPathsToDelete.forEach {
            val musicFile = File(it)
            if(musicFile.delete()) {
                logger.info("Successfully deleted $musicFile.")
            } else {
                logger.info("Failed to delete $musicFile.")
            }
        }



        database.getCollection<Song>(TableName.SONGS.tableName).deleteMany(
            Filters.`in`("_id", songsToRemove)
        )

        database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).deleteMany(
            Filters.`in`("songId", songsToRemove)
        )

        albumIds.forEach {
            if(database.getCollection<Song>(TableName.SONGS.tableName).find(Filters.eq("albumId", it)).toList().isEmpty()) {
                database.getCollection<Album>(TableName.ALBUMS.tableName).deleteOne(Filters.eq("_id", it))
                val file = File("$musicRootPath/${it}/")
                if(file.deleteRecursively())  {
                    logger.info("Successfully deleted $file.")
                } else {
                    logger.info("Failed to delete $file.")
                }

            }
        }

        artistIds.forEach {
            if(database.getCollection<Song>(TableName.SONGS.tableName).find(Filters.eq("artistId", it)).toList().isEmpty()) {
                database.getCollection<Artist>(TableName.ARTISTS.tableName).deleteOne(Filters.eq("_id", it))
            }
        }
    }

    internal suspend fun deletePlaylist(playlist: SharedPlaylist) {
        database.getCollection<Playlist>(TableName.PLAYLISTS.tableName).deleteMany(
            Filters.eq("_id", ObjectId(playlist.id))
        )

        database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).deleteMany(
            Filters.eq("playlistId", ObjectId(playlist.id))
        )
    }

    internal suspend fun deletePlaylistCategory(playlistCategory: SharedPlaylistCategory) {
        database.getCollection<PlaylistCategory>(TableName.PLAYLIST_CATEGORIES.tableName).deleteMany(
            Filters.eq("_id", ObjectId(playlistCategory.id))
        )

        val playlistIds = database.getCollection<Playlist>(TableName.PLAYLISTS.tableName).find(
            Filters.eq("categoryId", ObjectId(playlistCategory.id))
        ).toList().map { it.id }

        database.getCollection<Playlist>(TableName.PLAYLISTS.tableName).deleteMany(
            Filters.eq("categoryId", ObjectId(playlistCategory.id))
        )

        database.getCollection<PlaylistSong>(TableName.PLAYLIST_SONGS.tableName).deleteMany(
            Filters.`in`("playlistId", playlistIds)
        )
    }

    suspend fun isSongInRepertoire(albumId: ObjectId, artistId: ObjectId, fileName: String): Boolean {
        return database.getCollection<Song>(TableName.SONGS.tableName).find<Song>(
            Filters.and(
                Filters.eq("albumId", albumId),
                Filters.eq("artistId", artistId),
                Filters.eq("fileName", fileName)
            )
        ).toList().isNotEmpty()
    }
}
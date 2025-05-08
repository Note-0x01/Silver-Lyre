package com.silverlyre.database

import com.silverlyre.types.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

object ObjectIdSerializer : KSerializer<ObjectId> {
    override val descriptor = PrimitiveSerialDescriptor("ObjectId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(value.toHexString())
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        val stringValue = decoder.decodeString()
        try {
            return ObjectId(stringValue)
        } catch (e: IllegalArgumentException) {
            throw SerializationException("Failed to deserialize ObjectId: $stringValue", e)
        }
    }
}
enum class TableName(val tableName: String) {
    SONGS("Song"),
    ALBUMS("Album"),
    ARTISTS("Artist"),
    PLAYLIST_CATEGORIES("PlaylistCategory"),
    PLAYLISTS("Playlist"),
    PLAYLIST_SONGS("PlaylistSong")
}
interface Repertoire {
    fun convertToShared(): SharedRepertoire
}

@Serializable
data class Album(
    @BsonId
    @Serializable(ObjectIdSerializer::class)
    val id: ObjectId,
    val name: String,
) : Repertoire {
    override fun convertToShared(): SharedRepertoire {
        return SharedAlbum(id.toString(), name)
    }
}

@Serializable
data class Artist(
    @BsonId
    @Serializable(ObjectIdSerializer::class)
    val id: ObjectId,
    val name: String,
) : Repertoire {
    override fun convertToShared(): SharedRepertoire {
        return SharedArtist(id.toString(), name)
    }
}

@Serializable
data class Song(
    @BsonId
    @Serializable(ObjectIdSerializer::class)
    val id: ObjectId,
    val name: String,
    @Serializable(ObjectIdSerializer::class)
    val artistId: ObjectId,
    @Serializable(ObjectIdSerializer::class)
    val albumId: ObjectId,
    val order: Int,
    val fileName: String,
    val length: Double,
) : Repertoire {
    override fun convertToShared(): SharedRepertoire {
        return SharedSong(id.toString(), name, artistId.toString(), albumId.toString(), order, fileName, length)
    }
}

interface PlaylistTable : Repertoire

@Serializable
data class PlaylistCategory(
    @BsonId
    @Serializable(ObjectIdSerializer::class)
    val id: ObjectId,
    val name: String,
    val configuredBot: Int,
    val order: Int
) : PlaylistTable {
    override fun convertToShared(): SharedRepertoire {
        return SharedPlaylistCategory(id.toString(), name, configuredBot, order)
    }
}

@Serializable
data class Playlist(
    @BsonId
    @Serializable(ObjectIdSerializer::class)
    val id: ObjectId,
    @Serializable(ObjectIdSerializer::class)
    val categoryId: ObjectId,
    val name: String,
    val order: Int
) : PlaylistTable {
    override fun convertToShared(): SharedRepertoire {
        return SharedPlaylist(id.toString(), categoryId.toString(), name, order)
    }
}

@Serializable
data class PlaylistSong(
    @BsonId
    @Serializable(ObjectIdSerializer::class)
    val id: ObjectId,
    @Serializable(ObjectIdSerializer::class)
    val playlistId: ObjectId,
    @Serializable(ObjectIdSerializer::class)
    val songId: ObjectId,
    val order: Int
) : PlaylistTable {
    override fun convertToShared(): SharedRepertoire {
        return SharedPlaylistSong(id.toString(), playlistId.toString(), songId.toString(), order)
    }
}
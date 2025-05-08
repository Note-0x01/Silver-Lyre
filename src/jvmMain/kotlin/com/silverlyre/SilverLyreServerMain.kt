@file:Suppress("UNCHECKED_CAST")

package com.silverlyre

import com.natpryce.konfig.*
import com.silverlyre.database.DatabaseClient
import com.silverlyre.database.PlaylistManager
import com.silverlyre.database.TableName
import com.silverlyre.discord.BardBot
import com.silverlyre.ktor.Connection
import com.silverlyre.messages.*
import com.silverlyre.music.MusicPlayer
import com.silverlyre.music.MusicProcessor
import com.silverlyre.types.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet

import io.kvision.remote.kvisionInit
import org.bson.types.ObjectId

var bards = ArrayList<BardBot>()
val connections: MutableSet<Connection> = Collections.synchronizedSet(LinkedHashSet())
val format = Json { classDiscriminator = "type" }
var musicRootPath= ""

suspend fun sendMessage(message: Message) {
    connections.forEach {
        it.session.send(Frame.Text(
            format.encodeToString<Message>(message)))
    }
}

fun main() {
    val port = Key("port", intType)
    val databasePort = Key("database.port", intType)
    val primaryToken = Key("primary.token", stringType)
    val secondaryToken = Key("secondary.token", stringType)
    val musicStoragePath = Key("music.storagePath", stringType)
    val user = Key("auth.user", stringType)
    val pass = Key("auth.pass", stringType)

    val logger: Logger = LoggerFactory.getLogger("BardServerMain")
    logger.info("BardKt is starting up!")
    logger.info("Reading config")
    val config = ConfigurationProperties.systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromFile(File("bard.properties")) overriding
            ConfigurationProperties.fromResource("defaults.properties")

    if(config[primaryToken].isBlank()) {
        logger.error("No primary bot token set, please configure one in your bard.properties file.")
        return
    }

    musicRootPath = config[musicStoragePath]

    logger.info("Connecting to DB.")
    val connectionString = "mongodb://192.168.2.40:${config[databasePort]}"
    val databaseClient = DatabaseClient(connectionString)
    databaseClient.start()

    logger.info("Disabling unwanted logs.")
    val disabledLogger = arrayOf( java.util.logging.Logger.getLogger("org.jaudiotagger") )
    disabledLogger.forEach { it.level = java.util.logging.Level.OFF }

    logger.info("Starting up MusicProcessor.")
    val musicProcessor = MusicProcessor(databaseClient)
    musicProcessor.start()

    val playlistManager = PlaylistManager(databaseClient)

    // Starts up the Discord bot threads
    bards.add(BardBot(config[primaryToken]))
    bards[0].start()

    if(config[secondaryToken].isNotBlank()) {
        bards.add(BardBot(config[secondaryToken], isSecondary = true))
        bards[1].start()
    } else {
        logger.info("No secondary bot token set, ignoring.")
    }

   //Files.walk(Path("testmusic\\")).forEach {
   //    musicProcessor.addToQueue(it.toFile())
   //}

    logger.info("Starting up MusicPlayer.")
    for(bard in bards) {
        bard.musicPlayer = MusicPlayer(bard, databaseClient)
        bard.musicPlayer!!.start()
    }

    logger.info("Starting ktor server.")
    embeddedServer(Netty, port = config[port]) {
        install(WebSockets)
        install(StatusPages) {
            status(HttpStatusCode.NotFound) { call, _ ->
                call.respondFile(File("${config[musicStoragePath]}/Other/Other.jpg"))
            }
        }
        install(Authentication) {
            basic("auth-form") {
                validate { credentials ->
                    if (credentials.name == config[user] && credentials.password == config[pass]) {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }


        routing {
            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
            }

            authenticate("auth-form") {
                post("/login") {
                    call.respondText("Log in was successful!")
                }

                post("/upload") {
                    logger.info("Received upload post with music!")
                    val multipartData = call.receiveMultipart()
                    var fileToProcess = ""
                    multipartData.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val fileName = part.originalFileName as String
                                val fileBytes = part.streamProvider().readBytes()
                                try {
                                    File("uploads/$fileName").writeBytes(fileBytes)
                                } catch (e: Exception) {
                                    logger.info("Uploaded file failed to save: ${e.localizedMessage}")
                                }
                                fileToProcess = fileName
                            }

                            else -> {}
                        }
                        part.dispose()
                    }

                    musicProcessor.addToQueue(File("uploads/$fileToProcess"))
                    call.respondText { "{}" }
                }
                webSocket("/socket") {
                    val thisConnection = Connection(this)
                    connections += thisConnection
                    sendLibraryUpdate(databaseClient)
                    sendBardUpdate()
                    sendDiscordChannelInformation()

                    try {
                        for (message in incoming) {
                            val othersMessage = message as? Frame.Text ?: continue
                            when (val receivedMessage = Json.decodeFromString<Message>(othersMessage.readText())) {
                                is LibraryMessage -> sendLibraryUpdate(databaseClient)
                                is RenameItemsMessage -> databaseClient.renameItems(receivedMessage.itemsToUpdate)
                                is RemoveSelectedFromLibraryMessage -> databaseClient.removeItemsFromLibrary(
                                    receivedMessage.itemsToRemove
                                )

                                is ModifyManualQueueMessage -> {
                                    when (receivedMessage.modificationType) {
                                        ModificationType.ADD -> {
                                            runBlocking {
                                                when (receivedMessage.songsToModify.firstOrNull()) {
                                                    is SharedAlbum -> {
                                                        val songs =
                                                            databaseClient.repertoireHandler.getAllSongsInAlbumsSharedAsSongsShared(
                                                                receivedMessage.songsToModify.map { ObjectId(it.id) })
                                                        bards[receivedMessage.bot.id].musicPlayer!!.addSongs(songs)
                                                    }

                                                    is SharedArtist -> {
                                                        val songs =
                                                            databaseClient.repertoireHandler.getAllSongsInArtistsSharedAsSongsShared(
                                                                receivedMessage.songsToModify.map { ObjectId(it.id) })
                                                        bards[receivedMessage.bot.id].musicPlayer!!.addSongs(songs)
                                                    }

                                                    is SharedPlaylist -> TODO()
                                                    is SharedPlaylistCategory -> TODO()
                                                    is SharedSong -> {
                                                        bards[receivedMessage.bot.id].musicPlayer!!.addSongs(
                                                            receivedMessage.songsToModify as List<SharedSong>
                                                        )
                                                    }

                                                    null -> logger.info("Songs to modify for manual queue empty.")
                                                }
                                            }
                                        }

                                        ModificationType.REMOVE -> {
                                            runBlocking {
                                                receivedMessage.fistPos?.let {
                                                    bards[receivedMessage.bot.id].musicPlayer!!.removeSongFromManualQueue(
                                                        it
                                                    )
                                                }
                                            }
                                        }

                                        ModificationType.REORDER -> TODO()
                                        ModificationType.CLEAR -> bards[receivedMessage.bot.id].musicPlayer!!.clearManualQueue()
                                    }
                                    sendBardUpdate()
                                }

                                is ModifyCollectionQueueMessage -> {
                                    when (receivedMessage.modificationType) {
                                        ModificationType.ADD -> {
                                            runBlocking {
                                                bards[receivedMessage.bot.id].musicPlayer!!.queueCollectionFromPoint(
                                                    receivedMessage.songsToModify[0],
                                                    receivedMessage.playlistId
                                                )
                                            }
                                        }

                                        ModificationType.REMOVE -> {
                                            runBlocking {
                                                receivedMessage.fistPos?.let {
                                                    bards[receivedMessage.bot.id].musicPlayer!!.removeSongFromCollectionQueue(
                                                        it
                                                    )
                                                }
                                            }
                                        }

                                        ModificationType.REORDER -> TODO()
                                        ModificationType.CLEAR -> bards[receivedMessage.bot.id].musicPlayer!!.clearCollectQueue()
                                    }
                                    sendBardUpdate()
                                }

                                is RetrieveBotInformationMessage -> {
                                    sendBardUpdate()
                                }

                                is ControlMusicPlayerMessage -> {
                                    val player = bards[receivedMessage.bot.id].musicPlayer!!
                                    when (receivedMessage.musicPlayerControl) {
                                        MusicPlayerControl.PLAY_PAUSE -> player.playOrPause()
                                        MusicPlayerControl.BACK -> player.playLastSong()
                                        MusicPlayerControl.FORWARD -> player.playNextSong()
                                        MusicPlayerControl.SHUFFLE -> player.shuffleQueue()
                                        MusicPlayerControl.REPEAT -> player.setRepeat()
                                        MusicPlayerControl.SEEK -> receivedMessage.seekPoint?.let { player.seekTrack(it) }
                                    }
                                    sendBardUpdate()
                                }

                                is JoinChannelMessage -> {
                                    bards[receivedMessage.bot.id].joinChannel(receivedMessage.channelId)
                                }

                                is PlaylistMessage -> {
                                    playlistManager.processPlaylistMessage(receivedMessage)
                                }

                                else -> {
                                    logger.info("Received unexpected message type.")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(e.localizedMessage)
                    } finally {
                        connections -= thisConnection
                    }
                }
                staticFiles("/music", File(config[musicStoragePath])) {
                    enableAutoHeadResponse()
                }
                staticFiles("/", File("front-end"))
            }
        }
        kvisionInit()
    }.start(wait = true)
}

suspend fun sendLibraryUpdate(databaseClient: DatabaseClient) {
    sendMessage(
        LibraryMessage(
            databaseClient.getAllOfTypeShared(TableName.SONGS) as List<SharedSong>,
            databaseClient.getAllOfTypeShared(TableName.ALBUMS) as List<SharedAlbum>,
            databaseClient.getAllOfTypeShared(TableName.ARTISTS) as List<SharedArtist>,
            databaseClient.getAllOfTypeShared(TableName.PLAYLIST_CATEGORIES) as List<SharedPlaylistCategory>,
            databaseClient.getAllOfTypeShared(TableName.PLAYLISTS) as List<SharedPlaylist>,
            databaseClient.getAllOfTypeShared(TableName.PLAYLIST_SONGS) as List<SharedPlaylistSong>
        )
    )
}

private suspend fun sendDiscordChannelInformation() {
    sendMessage(
        DiscordGuildInfoMessage(
            bards[0].getGuildsAndChannels(0) + bards[1].getGuildsAndChannels(1)
        )
    )
}

suspend fun sendBardUpdate() {
    for ((id, bard) in bards.withIndex()) {
        runBlocking {
            sendMessage(
                RetrieveBotInformationMessage(
                    SharedBot(
                        id,
                        bard.bardState,
                        bard.musicPlayer!!.isPlaying,
                        bard.musicPlayer!!.trackPlaying,
                        bard.musicPlayer!!.getPosition(),
                        bard.musicPlayer!!.repeat,
                        bard.musicPlayer!!.shuffleSong,
                        bard.musicPlayer!!.currentSong,
                        bard.musicPlayer!!.getCurrentManualQueue(),
                        bard.musicPlayer!!.collectionPosition,
                        bard.musicPlayer!!.getCurrentCollectionQueue()
                    )
                )
            )
        }
    }
}
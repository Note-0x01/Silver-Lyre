package com.silverlyre.project

import com.silverlyre.messages.*
import com.silverlyre.project.elements.body
import com.silverlyre.project.elements.foot
import com.silverlyre.project.elements.head
import com.silverlyre.types.SharedSong
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.kvision.*
import io.kvision.core.Background
import io.kvision.core.Overflow
import io.kvision.core.onEvent
import io.kvision.html.*
import io.kvision.modal.Alert
import io.kvision.state.*
import io.kvision.panel.root
import io.kvision.utils.vh
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val AppScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
private val client = HttpClient() {
    install(WebSockets)
}

private var socket: WebSocketSession? = null
fun sendMessage(message: Message) {
    AppScope.launch {
        socket?.send(Frame.Text(Json.encodeToString<Message>(message)))
        cancel()
    }
}

private var shouldRestart = true


class App : Application() {

    private suspend fun DefaultClientWebSocketSession.processIncomingMessages() {
        try {
            for (message in incoming) {
                val othersMessage = message as? Frame.Text ?: continue

                when (val incomingMessage = Json.decodeFromString<Message>(othersMessage.readText())) {
                    is LibraryMessage -> repertoire.updateLibrary(incomingMessage)
                    is RetrieveBotInformationMessage -> incomingMessage.bot?.let {
                        if(it.id >= botsToManage.size)
                            botsToManage.add(it)
                        else
                            botsToManage[it.id] = it

                        if(botsToManage.isNotEmpty()) {
                            if(botsToManage[0].currentSong != null) {
                                currentSongLength.setState((botsToManage[0].currentSong?.length!! * 1000).toLong())
                            }

                            currentSongPosition.setState(botsToManage[0].position)
                        }
                    }
                    is DiscordGuildInfoMessage -> {
                        discordGuilds.clear()
                        incomingMessage.guilds.let { discordGuilds.addAll(it) }
                    }
                    else -> console.log("Unexpected Message Type Received: $incomingMessage")
                }

                return
            }
        } catch(_: CancellationException) {
        } catch (e: Exception) {
            println("Error while receiving: ${e.message}")
        }
    }

    override fun start(state: Map<String, Any>) {
        AppScope.launch {
            while (shouldRestart){
                try {
                    client.webSocket(method = HttpMethod.Get, host = window.location.hostname, port = 8000, path = "/socket") {
                        socket = this
                        while (true) {
                            val incomingRoutine = launch {processIncomingMessages()}

                            incomingRoutine.join()
                        }
                    }
                } catch (e:Exception){
                    Alert.show("Websocket has disconnected from the server! Attempting to reconnect!.")

                    val waitFor=1000L
                    console.log("failed trying in $waitFor ms")
                    delay(waitFor)
                }
            }

            client.close()
        }

        startTimer()

        //Construct Page
        root("root").bind(currentView) {
            overflow = Overflow.HIDDEN
            maxHeight = 100.vh
            div {
                background = Background(SilverLyreColours.MAIN_BG.colour)
                color = SilverLyreColours.TEXT.colour
                head()
                body()
                foot()
            }

            onEvent {
                keydown = {
                    if(it.ctrlKey)
                        ctrlPressed = true

                    if(it.shiftKey)
                        shiftPressed = true
                }

                keyup = {
                    if(it.ctrlKey)
                        ctrlPressed = false

                    if(it.shiftKey)
                        shiftPressed = false
                }
            }
        }
    }

    private fun startTimer() {
        AppScope.launch {
            var currentSong: SharedSong? = null
            while (true) {
                if(botsToManage.isNotEmpty()) {
                    if(shouldTimerDoStuff) {
                        if (botsToManage[0].currentSong != currentSong) {
                            if(currentSong != null) {
                                currentSongLength.setState((currentSong.length * 1000).toLong())
                            }
                            currentSong = botsToManage[0].currentSong
                            currentSongPosition.setState(botsToManage[0].position)
                        } else if(currentSongPosition.getState() < currentSongLength.getState() && botsToManage[0].currentSong == currentSong && !botsToManage[0].isPlaying) {
                            currentSongPosition.setState(currentSongPosition.getState()+10)
                        } else if (botsToManage[0].position != currentSongPosition.getState()) {
                            currentSongPosition.setState(botsToManage[0].position)
                        }
                    }
                }
                delay(10)
            }
        }
    }
}

fun main() {
    startApplication(
        ::App,
        module.hot,
        FontAwesomeModule,
        BootstrapModule,
        BootstrapCssModule,
        BootstrapUploadModule,
        CoreModule
    )
}

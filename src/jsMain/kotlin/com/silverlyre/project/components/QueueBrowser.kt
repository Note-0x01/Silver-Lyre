package com.silverlyre.project.components

import com.silverlyre.messages.ModificationType
import com.silverlyre.messages.ModifyCollectionQueueMessage
import com.silverlyre.messages.ModifyManualQueueMessage
import com.silverlyre.project.*
import com.silverlyre.types.SharedBot
import com.silverlyre.types.SharedSong
import csstype.Border
import csstype.None
import io.kvision.core.*
import io.kvision.html.*
import io.kvision.state.bind
import io.kvision.utils.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.dom.addClass
import kotlinx.dom.removeClass

fun Div.queueBrowser() {
    div().bind(botsToManage) {
        paddingLeft = 1.rem
        if(1 < it.size) {
            div {
                renderQueueForBot(it[1])
                if (it[1].currentSong != null) {
                    div {
                        display = Display.FLEX
                        flexDirection = FlexDirection.COLUMN
                        float = PosFloat.LEFT
                        div {
                            marginLeft = 0.5.rem
                            display = Display.FLEX
                            flexDirection = FlexDirection.ROW
                            image("http://${window.location.hostname}:8000/music/${it[1].currentSong!!.albumId}/${it[1].currentSong!!.albumId}.jpg") {
                                width = 4.rem
                                height = 4.rem
                                marginTop = (0.5).rem
                            }
                            div {
                                marginTop = 0.5.rem
                                marginLeft = 0.5.rem
                                width = 16.rem
                                h5(it[1].currentSong!!.name) {
                                    id = "playing-song-name"
                                    color = SilverLyreColours.HOVER_TEXT.colour
                                    overflow = Overflow.HIDDEN
                                    textOverflow = TextOverflow.ELLIPSIS
                                    whiteSpace = WhiteSpace.NOWRAP
                                }
                                h6(repertoire.getAlbumFromId(it[1].currentSong!!.albumId).name) {
                                    id = "playing-song-album"
                                    overflow = Overflow.HIDDEN
                                    textOverflow = TextOverflow.ELLIPSIS
                                    color = SilverLyreColours.MUTED_TEXT.colour
                                    whiteSpace = WhiteSpace.NOWRAP
                                }
                            }
                        }
                    }
                }
                div {
                    songPlaybackButtons(it[1], 30.px) {
                        position = Position.ABSOLUTE
                        left = 0.px
                        right = 0.px
                        marginLeft = auto
                        setStyle("margin-right", "calc(${(RIGHT_COLUMN.first.toInt()/2).rem.asString()} - (${(30*5).px.asString()} / 2))")
                        width = (30*5).px
                        marginTop = 4.5.rem
                    }
                }
            }
        }

        if(it.isNotEmpty()) {
            div {
                position = Position.ABSOLUTE
                bottom = (FOOTER_SIZE.first.toDouble() + 0.5).rem
                renderQueueForBot(it[0])
            }
        }
    }
}

private fun Div.renderQueueForBot(bot: SharedBot) {
    div {
        div {
            width = (RIGHT_COLUMN.first.toInt() - 2).rem
            display = Display.FLEX
            flexDirection = FlexDirection.ROW
            flexGrow = 1
            h5(bot.name) {
                marginRight = auto
            }
            if(bot.manualQueue?.isNotEmpty() == true) {
                h6("Clear manual queue") {
                    addCssClass("clearQueue")
                    color = SilverLyreColours.MUTED_TEXT.colour
                    onEvent {
                        mouseover = {
                            color = Color(Col.LIGHTGRAY.name)
                        }
                        mouseleave = {
                            color = SilverLyreColours.MUTED_TEXT.colour
                        }
                    }

                    onClick {
                        sendMessage(
                            ModifyManualQueueMessage(
                                bot,
                                ModificationType.CLEAR,
                                listOf()
                            )
                        )
                    }
                }
            }
        }

        div {
            background = Background(color = SilverLyreColours.SECONDARY_BG.colour)
            border = Border(3.px, BorderStyle.INSET, SilverLyreColours.HOVER_ITEM.colour)
            id = "${bot.name}-queue"
            height = 30.vh
            width = (RIGHT_COLUMN.first.toInt() - 2).rem
            overflowY = Overflow.SCROLL
            addCssClass("scroll-gold")
            overflowX = Overflow.HIDDEN
            div {
                bot.collectionQueue?.let { queue ->
                    if (queue.isNotEmpty()) {
                        flexDirection = FlexDirection.COLUMNREV
                        display = Display.FLEX
                        renderQueue(bot, queue, false)
                    }
                }
            }
            div {
                bot.manualQueue?.let { queue ->
                    if (queue.isNotEmpty()) {
                        borderBottom =
                            Border(2.px, BorderStyle.SOLID, Color(SilverLyreColours.HOVER_ITEM.colour.asString()))
                        flexDirection = FlexDirection.COLUMNREV
                        display = Display.FLEX
                        renderQueue(bot, queue, true)
                    }

                }
            }
        }

        val element = document.getElementById("${bot.name}-queue")
        if (element != null) {
            if (element.scrollHeight - element.clientHeight.toDouble() <= element.scrollTop + 1) {
                AppScope.launch {
                    element.scrollTop = element.scrollHeight.toDouble()
                }
            }
        }
    }
}

private fun Div.renderQueue(
    bot: SharedBot,
    collectionQueue: List<SharedSong>,
    isManual: Boolean
) {
    for ((queuePos, song) in collectionQueue.withIndex()) {
        if ((queuePos > bot.collectionPosition!! || (bot.collectionPosition == collectionQueue.size - 1 && bot.shuffleEnabled)) || isManual) {
            div {
                border = Border(3.px, BorderStyle.OUTSET, SilverLyreColours.TERTIARY_BG.colour)
                margin = 0.px
                display = Display.TABLE
                height = 40.px
                width = 100.perc
                div {
                    id = "queue-label"
                    float = PosFloat.LEFT
                    display = Display.TABLECELL
                    overflow = Overflow.HIDDEN
                    addCssClass("song-queue-div")
                    image("http://${window.location.hostname}:8000/music/${song.albumId}/${song.albumId}.jpg") {
                        width = 40.px
                        height = 40.px
                        margin = 0.px
                    }
                    h6(song.name) {
                        width = (RIGHT_COLUMN.first.toInt() - 10).rem
                        marginLeft = 0.5.rem
                        id = "queue-${song.name}-label"
                        display = Display.INLINEBLOCK
                        whiteSpace = WhiteSpace.NOWRAP
                        overflow = Overflow.HIDDEN
                        textOverflow = TextOverflow.ELLIPSIS
                    }
                }
                div {
                    float = PosFloat.RIGHT
                    display = Display.TABLECELL
                    button("") {
                        setStyle("border", "none")
                        setStyle("background", "none")
                        border = Border(0.px)
                        height = 40.px
                        image = io.kvision.require("icons/delete.svg") as? String
                        right = 0.px

                        addCssClass("icon-colour")

                        onClick {
                            if(isManual) {
                                sendMessage(
                                    ModifyManualQueueMessage(
                                        bot,
                                        ModificationType.REMOVE,
                                        listOf(song),
                                        fistPos = queuePos
                                    )
                                )
                            } else {
                                sendMessage(
                                    ModifyCollectionQueueMessage(
                                        bot,
                                        ModificationType.REMOVE,
                                        listOf(song),
                                        fistPos = queuePos
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if(isManual) {
        div {
            h6("Manual Queue")
        }
    }
}


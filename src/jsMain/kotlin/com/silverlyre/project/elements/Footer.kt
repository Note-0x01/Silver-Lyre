package com.silverlyre.project.elements

import com.silverlyre.messages.ControlMusicPlayerMessage
import com.silverlyre.messages.MusicPlayerControl
import com.silverlyre.project.*
import com.silverlyre.project.components.songPlaybackButtons
import io.kvision.core.*
import io.kvision.form.number.range
import io.kvision.html.*
import io.kvision.state.bind
import io.kvision.utils.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.dom.addClass

fun Div.foot() {
    footer {
        addCssClass("grid-item")
        position = Position.STICKY
        background = Background(color = SilverLyreColours.SECONDARY_BG.colour)
        bottom = 0.px
        height = FOOTER_SIZE
        display = Display.INLINEFLEX
        div().bind(botsToManage) {
            width = 11.vw
            if(it.isNotEmpty()) {
                if (it[0].currentSong != null) {
                    image("http://${window.location.hostname}:8000/music/${it[0].currentSong!!.albumId}/${it[0].currentSong!!.albumId}.jpg") {
                        position = Position.ABSOLUTE
                        bottom = 0.1.rem
                        maxWidth = 10.vw
                        maxHeight = 10.vw
                        marginLeft = 0.5.rem
                        marginBottom = 0.5.rem
                    }
                }
            }
        }

        div().bind(botsToManage) {
            display = Display.FLEX
            setStyle("width", "calc(100vw - 10vw)")
            div {
                id = "playing-box"
                width = 30.vw
                float = PosFloat.LEFT
                if(it.isNotEmpty()) {
                    it[0].currentSong?.let { song ->
                        h4(song.name) {
                            id = "playing-song-name"
                            color = SilverLyreColours.HOVER_TEXT.colour
                            overflow = Overflow.HIDDEN
                            textOverflow = TextOverflow.ELLIPSIS
                            whiteSpace = WhiteSpace.NOWRAP
                        }
                        h5(repertoire.getAlbumFromId(it[0].currentSong!!.albumId).name) {
                            id = "playing-song-album"
                            overflow = Overflow.HIDDEN
                            textOverflow = TextOverflow.ELLIPSIS
                            color = SilverLyreColours.MUTED_TEXT.colour
                            whiteSpace = WhiteSpace.NOWRAP
                        }
                    }
                }
            }
            if(it.isNotEmpty()) {
                alignItems = AlignItems.CENTER
                div {
                    bottom = 2.5.rem
                    position = Position.ABSOLUTE
                    left = 0.px
                    right = 0.px
                    marginLeft = auto
                    marginRight = auto
                    width = (40*5).px
                    songPlaybackButtons(it[0], 40.px) {}
                }
                div {
                    left = 0.px
                    right = 0.px
                    bottom = 0.px
                    marginBottom = 0.1.rem
                    marginLeft = auto
                    marginRight = auto
                    width = ((40*5)*5).px
                    if (it[0].currentSong != null) {
                        range(min = 0, max = (botsToManage[0].currentSong?.length!! * 1000).toLong(), step = 1).bind(
                            currentSongPosition
                        ) {pos ->
                            addCssClass("seekBar")
                            position = Position.ABSOLUTE
                            left = 0.px
                            right = 0.px
                            marginLeft = auto
                            marginRight = auto
                            bottom = 0.px
                            marginBottom = 0.1.rem
                            width = ((40*5)*5).px

                            value = pos
                        }.onEvent {
                            mousedown = {
                                shouldTimerDoStuff = false
                            }

                            mouseup = {
                                shouldTimerDoStuff = true
                            }

                            click = {
                                val inputPos = this.self.value?.toLong()
                                sendMessage(
                                    ControlMusicPlayerMessage(
                                        botsToManage[0],
                                        MusicPlayerControl.SEEK,
                                        inputPos
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
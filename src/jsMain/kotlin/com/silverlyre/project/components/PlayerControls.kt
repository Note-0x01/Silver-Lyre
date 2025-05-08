package com.silverlyre.project.components

import com.silverlyre.messages.ControlMusicPlayerMessage
import com.silverlyre.messages.MusicPlayerControl
import com.silverlyre.project.sendMessage
import com.silverlyre.types.RepeatType
import com.silverlyre.types.SharedBot
import io.kvision.core.CssSize
import io.kvision.core.onClick
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.html.image

fun Div.songPlaybackButtons(bot: SharedBot?, iconSize: CssSize, divSettings: () -> Unit) {
    if (bot != null) {
        div {
            divSettings()
            image("/icons/shuffle.svg") {
                width = iconSize
                height = iconSize
                if (bot.shuffleEnabled) {
                    addCssClass("icon-set")
                } else {
                    addCssClass("icon-colour")
                }
                onClick {
                    sendMessage(ControlMusicPlayerMessage(bot, MusicPlayerControl.SHUFFLE))
                }
            }
            image("/icons/skip-back.svg") {
                width = iconSize
                height = iconSize
                addCssClass("icon-colour")
                onClick {
                    sendMessage(ControlMusicPlayerMessage(bot, MusicPlayerControl.BACK))
                }
            }
            image("/icons/pause.svg") {
                width = iconSize
                height = iconSize
                if (bot.isPlaying) {
                    src = "/icons/play.svg"
                    addCssClass("icon-colour")
                    removeCssClass("icon-set")
                } else {
                    removeCssClass("icon-colour")
                    addCssClass("icon-set")
                }
                onClick {
                    sendMessage(ControlMusicPlayerMessage(bot, MusicPlayerControl.PLAY_PAUSE))
                }
            }
            image("/icons/skip-forward.svg") {
                width = iconSize
                height = iconSize
                addCssClass("icon-colour")
                onClick {
                    sendMessage(ControlMusicPlayerMessage(bot, MusicPlayerControl.FORWARD))
                }
            }

            image("/icons/repeat.svg") {
                width = iconSize
                height = iconSize
                when (bot.repeatEnabled) {
                    RepeatType.NO -> addCssClass("icon-colour")
                    RepeatType.QUEUE -> addCssClass("icon-set")
                    RepeatType.SONG -> {
                        addCssClass("icon-set")
                        src = "/icons/repeat-1.svg"
                    }
                }

                onClick {
                    sendMessage(ControlMusicPlayerMessage(bot, MusicPlayerControl.REPEAT))
                }
            }
        }
    }
}

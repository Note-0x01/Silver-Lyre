package com.silverlyre.project.elements

import com.silverlyre.messages.JoinChannelMessage
import com.silverlyre.project.*
import com.silverlyre.types.SearchForm
import io.kvision.core.*
import io.kvision.dropdown.dropDown
import io.kvision.form.FormType
import io.kvision.form.formPanel
import io.kvision.form.text.Text
import io.kvision.form.text.text
import io.kvision.html.*
import io.kvision.panel.hPanel
import io.kvision.state.bind
import io.kvision.utils.perc
import io.kvision.utils.px
import io.kvision.utils.rem

fun Div.head() {
    header {
        position = Position.STICKY
        height = HEADER_SIZE
        width = 100.perc
        display = Display.INLINEFLEX
        background = Background(color = SilverLyreColours.SECONDARY_BG.colour)
        h1 {
            padding = 0.5.rem
            fontFamily = "Whisper, cursive"
            content = "Silver Lyre"
        }

        lateinit var searchTerm: Text
        formPanel<SearchForm>(type = FormType.HORIZONTAL) {
            margin = 0.px
            maxWidth = 60.rem
            hPanel() {
                searchTerm = text {
                    marginLeft = 10.rem
                    width = 60.rem
                    padding = 1.rem
                    placeholder = "Search by artists, songs, or albums"
                }
            }
        }.onEvent {
            keypress = {
                if (it.key == "Enter") {
                    it.preventDefault()
                    currentView.value = OpenRepertoire.SEARCH
                    clientData["searchTerm"] = searchTerm.value!!
                }
            }
        }

        div().bind(discordGuilds) {
            marginTop = 1.rem
            botsToManage.forEach { bot ->
                display = Display.FLEX
                flexDirection = FlexDirection.ROW
                dropDown("Connect ${bot.name}", forNavbar = true) {
                    display = Display.FLEX
                    flexDirection = FlexDirection.COLUMN
                    div {
                        display = Display.FLEX
                        flexDirection = FlexDirection.COLUMN
                        it.filter { it.botId == bot.id }.forEach { guild ->
                            header(guild.name)
                            guild.channels.forEach { channel ->
                                button(channel.name, style = ButtonStyle.OUTLINEPRIMARY) {
                                    height = 30.px
                                    marginTop = 5.px
                                    paddingTop = 0.px
                                    textAlign = TextAlign.LEFT
                                    textOverflow = TextOverflow.ELLIPSIS
                                    whiteSpace = WhiteSpace.NOWRAP
                                    onClick {
                                        sendMessage(
                                            JoinChannelMessage(
                                                bot,
                                                channel.id
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
    }
}
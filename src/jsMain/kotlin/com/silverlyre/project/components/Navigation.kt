package com.silverlyre.project.components

import com.silverlyre.project.*
import io.kvision.core.onEvent
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.html.h2
import io.kvision.utils.rem
import kotlinx.browser.document

fun Div.leftMenuNavButton(view: OpenRepertoire) {
    div {
        margin = 0.rem
        width = 20.rem
        padding = 0.1.rem
        h2(view.prettyName).onEvent {
            paddingLeft = 1.rem
            click = {
                changeView(view)
                document.getElementById("repertoireList")?.scrollTop = 0.0
            }
            mouseover = {
                setStyle("background-image", "linear-gradient(to right, ${SilverLyreColours.SECONDARY_BG.colour}, ${SilverLyreColours.MAIN_BG.colour})")
                color = SilverLyreColours.HOVER_TEXT.colour
            }
            mouseleave = {
                setStyle("background-image", "none")
                color = SilverLyreColours.TEXT.colour
            }
        }
    }
}

fun Div.navigationMenu() {
    div {
        paddingTop = 1.rem
        leftMenuNavButton(OpenRepertoire.ALBUMS)
        leftMenuNavButton(OpenRepertoire.ARTISTS)
        leftMenuNavButton(OpenRepertoire.UPLOAD)
    }
}
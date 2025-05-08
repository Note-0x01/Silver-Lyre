package com.silverlyre.project.elements

import com.silverlyre.project.*
import com.silverlyre.project.components.*
import io.kvision.core.*
import io.kvision.dropdown.*
import io.kvision.html.*
import io.kvision.html.header
import io.kvision.utils.*

fun Section.left() {
    div {
        float = PosFloat.LEFT
        width = LEFT_COLUMN
        margin = 0.px
        navigationMenu()
        playlistMenu()
    }
}

fun Section.middle() {
    div {
        margin = 0.px
        float = PosFloat.LEFT
        setStyle("width", "calc(100vw - ${LEFT_COLUMN.first.toInt() + RIGHT_COLUMN.first.toInt()}rem)")
        displayCurrentView()
    }
}

fun Section.right() {
    div {
        float = PosFloat.LEFT
        width = RIGHT_COLUMN
        margin = 0.px
        marginTop = 1.rem
        padding = 0.px
        queueBrowser()
    }
}

fun Div.body() {
    section {
        display = Display.INLINEFLEX
        setStyle("height", "calc(100vh - ${HEADER_SIZE.asString()} - ${FOOTER_SIZE.asString()})")
        left()
        middle()
        right()
    }
}
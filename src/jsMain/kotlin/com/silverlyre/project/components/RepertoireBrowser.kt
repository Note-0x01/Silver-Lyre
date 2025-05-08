package com.silverlyre.project.components

import com.silverlyre.messages.*
import com.silverlyre.project.*
import com.silverlyre.types.*
import io.kvision.core.*
import io.kvision.dropdown.*
import io.kvision.form.FormPanel
import io.kvision.form.form
import io.kvision.form.text.text
import io.kvision.form.upload.bootstrapUpload
import io.kvision.html.*
import io.kvision.modal.Modal
import io.kvision.state.bind
import io.kvision.utils.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import org.w3c.dom.Element
import org.w3c.dom.asList

private fun interactRepertoire(item: SharedRepertoire, botId: Int) {
    if (botsToManage.isNotEmpty()) {
        when(item) {
            is SharedSong -> {
                if(currentView.value == OpenRepertoire.PLAYLIST) {
                    sendMessage(
                        ModifyCollectionQueueMessage(
                            botsToManage[botId],
                            ModificationType.ADD,
                            listOf(item),
                            playlistId = selectedPlaylist.getState()?.id))
                } else {
                    sendMessage(
                        ModifyCollectionQueueMessage(
                            botsToManage[botId],
                            ModificationType.ADD,
                            listOf(item)))
                }
            } else -> {
                document.getElementById("repertoireList")?.scrollTop = 0.0
                clientData["currentDisc"] = item
                currentView.value = OpenRepertoire.SONGS
            }
        }
    }
}

private fun selectRepertoireItem(mouseButton: Int, item: SharedRepertoireItem, ctrlPressed: Boolean, shiftPressed: Boolean, botId: Int) {
    if (ctrlPressed) {
        if(selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
    } else {
        if(selectedItems.contains(item)) {
            if(mouseButton == 0) {
                interactRepertoire(item, botId)
            }
        } else {
            selectedItems.clear()
            selectedItems.add(item)
        }
    }
}

fun Div.showRepertoireItem(src: String, item: SharedRepertoireItem, botId: Int) {
    height = 3.5.rem

    image(src) {
        setAttribute("crossorigin", "")
        width = 3.5.rem
        height = 3.5.rem
        padding = 0.1.rem
        display = Display.INLINEFLEX
        float = PosFloat.LEFT
    }
    h5(item.name) {
        paddingTop = 1.rem
        paddingLeft = 0.5.rem
        display = Display.INLINEFLEX
    }

    onEvent {
        mouseover = {
            background = Background(SilverLyreColours.SELECTED.colour)
            color = SilverLyreColours.HOVER_TEXT.colour
        }
        mouseleave = {
            background = Background(SilverLyreColours.MAIN_BG.colour)
            color = SilverLyreColours.TEXT.colour
        }
        mouseup = {mouseUp ->
            selectRepertoireItem(mouseUp.button.toInt(), item, mouseUp.ctrlKey, mouseUp.shiftKey, botId)
        }
    }
}

fun Div.songsInRepertoire(inputList: List<SharedRepertoire>, botId: Int) {
    if (inputList.isEmpty())
        return

    id = "item-list"
    var readyList = inputList

    if (inputList.first() is SharedSong)
        readyList = (readyList.unsafeCast<List<SharedSong>>()).sortedBy { it.order }

    for (item in readyList) {
        div{
            id = item.id
            height = 3.5.rem
            maxHeight = 3.5.rem

            when (item) {
                is SharedSong -> {
                    val src = "http://${window.location.hostname}:8000/music/${item.albumId}/${item.albumId}.jpg"
                    showRepertoireItem(src, item, botId)
                }

                is SharedAlbum -> {
                    val src = "http://${window.location.hostname}:8000/music/${item.id}/${item.id}.jpg"
                    showRepertoireItem(src, item, botId)
                }

                is SharedArtist -> {
                    showRepertoireItem("http://${window.location.hostname}:8000/music/Other/Other.jpg", item, botId)
                }
            }
        }
    }
}

fun Div.displayCurrentView() {
    div {
        selectedItems.clear()

        id = "repertoireList"
        addCssClass("scroll-gold")
        overflowY = Overflow.SCROLL
        setStyle("height", "calc(100vh - ${HEADER_SIZE.asString()} - ${FOOTER_SIZE.asString()})")
        paddingTop = 1.rem
        fontFamily = "Inter, sans-serif"
        if(currentView.value == OpenRepertoire.ALBUMS) {
            div().bind(repertoire.albums) {
                songsInRepertoire(it, 0)
            }
        }

        if(currentView.value == OpenRepertoire.ARTISTS) {
            div().bind(repertoire.artists) {
                songsInRepertoire(it, 0)
            }
        }

        if(currentView.value == OpenRepertoire.SONGS) {
            val currentDisc = (clientData["currentDisc"] as SharedRepertoireItem)
            h4 (currentDisc.name)
            div().bind(repertoire.songs) {
                id = "item-list"
                songsInRepertoire(repertoire.songs.filter {it.compare(currentDisc)}, 0)
            }
        }

        if(currentView.value == OpenRepertoire.SEARCH) {
            div().bind(repertoire.songs) {
                id = "item-list"
                songsInRepertoire(repertoire.songs.filter {
                    it.name.lowercase().contains((clientData["searchTerm"] as String).lowercase())
                }, 0)
            }
        }

        if(currentView.value == OpenRepertoire.PLAYLIST) {
            div().bind(selectedPlaylist) {sharedPlaylist ->
                h4 ("${selectedPlaylistCategory.getState()?.name} - ${sharedPlaylist?.name}")
                div().bind(repertoire.playlistSongs) { playlistSongs ->
                    id = "item-list"
                    if (sharedPlaylist != null) {
                        val songs = playlistSongs.filter { it.playlistId == sharedPlaylist.id }
                        val songsInRepertoire = repertoire.songs
                        val songsWithOrder = songs.map { playlistSong ->
                            playlistSongToSong(playlistSong, songsInRepertoire.first { it.id == playlistSong.songId })
                        }
                        songsInRepertoire(songsWithOrder.sortedBy { it.order }, selectedPlaylistCategory.getState()!!.configuredBot)
                    }
                }
            }
        }

        if(currentView.value == OpenRepertoire.UPLOAD) {
            div {
                form() {
                    bootstrapUpload("http://${window.location.hostname}:8000/upload", multiple = true, label = "Upload songs") {
                        explorerTheme = false
                        dropZoneEnabled = true
                        allowedFileExtensions = setOf("mp3")
                    }
                }
            }
        } else {
            contextMenu {
                overflow = Overflow.VISIBLE
                addCssClass("dropright")
                addToPlaylistDropDown()
                dropDown("Add to queue", forDropDown = true) {
                    ddLink("Avarice").onClick {
                        sendMessage(
                            ModifyManualQueueMessage(
                                botsToManage[0],
                                ModificationType.ADD,
                                selectedItems.getState()
                            )
                        )
                    }
                    ddLink("Edea").onClick {
                        sendMessage(
                            ModifyManualQueueMessage(
                                botsToManage[1],
                                ModificationType.ADD,
                                selectedItems.getState()
                            )
                        )
                    }
                }
                if(currentView.getState() == OpenRepertoire.SONGS ||
                    currentView.getState() == OpenRepertoire.PLAYLIST ||
                    currentView.getState() == OpenRepertoire.SEARCH) {
                    cmLink("Play with Edea").onClick {
                        sendMessage(
                            ModifyCollectionQueueMessage(
                                botsToManage[1],
                                ModificationType.ADD,
                                listOf(selectedItems.getState().last() as SharedSong),
                                playlistId = selectedPlaylist.getState()?.id))
                    }
                    cmLink("Preview Track").onClick {
                        val firstSelectedItem = selectedItems.toList().first() as SharedSong
                        val loc = "http://${window.location.hostname}:8000/music/${firstSelectedItem.albumId}/${firstSelectedItem.fileName}"
                        url = loc
                        target="_blank"
                    }
                }
                cmLink("Edit").onClick {
                    //Change to form
                    val currentSelectedItems = selectedItems.toList()
                    val modal = Modal("Modify Selected Items")
                    val form: FormPanel<Map<String, Any?>> = form {
                        var index = 0;
                        div {
                            maxHeight = 40.vh
                            overflowY = Overflow.SCROLL
                            addCssClass("scroll-gold")
                            currentSelectedItems.forEach { selectedItem ->
                                text(value = selectedItem.name, label = selectedItem.name).bind(selectedItem.id)
                                    .onEvent {  ->
                                        keypress = {
                                            if (it.key == "Enter") {
                                                it.preventDefault()
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    modal.add(form)

                    modal.addButton(Button("Close") {
                        onClick {
                            modal.hide()
                        }
                    })

                    modal.addButton(Button("Confirm") {
                        onClick {
                            val finalItems = currentSelectedItems.map {
                                it.name = form.getData()[it.id].unsafeCast<String>(); it
                            }.filter { toFilter -> toFilter.name.isNotBlank() }
                            sendMessage(RenameItemsMessage(
                                finalItems
                            ))
                            selectedItems.clear()
                            modal.hide()
                        }
                    })

                    modal.show()
                }
                if(currentView.getState() == OpenRepertoire.PLAYLIST) {
                    cmLink("Remove from playlist").onClick {
                        if(selectedPlaylist.getState() != null) {
                            sendMessage(RemoveSongsFromPlaylistMessage(
                                selectedPlaylist.getState()!!,
                                selectedItems.toList().unsafeCast<List<SharedSong>>()
                            ))
                            selectedItems.clear()
                        }
                    }
                } else {
                    cmLink("Delete from library").onClick {
                        val modal = Modal("Delete these items from the library?")
                        val currentSelectedItems = selectedItems.toList()
                        modal.add(div {
                            maxHeight = 40.vh
                            overflowY = Overflow.SCROLL
                            addCssClass("scroll-gold")
                            currentSelectedItems.forEach { selectedItem ->
                                header(selectedItem.name)
                            }
                        })

                        modal.addButton(Button("Close") {
                            onClick {
                                modal.hide()
                            }
                        })

                        modal.addButton(Button("Confirm") {
                            onClick {
                                sendMessage(RemoveSelectedFromLibraryMessage(currentSelectedItems))
                                selectedItems.clear()
                                modal.hide()
                            }
                        })

                        modal.show()
                    }
                }
            }
        }
    }

    selectedItems.subscribe {selected ->
        val elements = mutableListOf<Element>()
        elements.addAll(document.getElementsByClassName("selectedRepertoireItem").asList())
        elements.forEach { element ->
            element.removeClass("selectedRepertoireItem")
        }

        val selectedIds = selectedItems.map { it.id }.toMutableList()
        selectedIds.forEach {
            document.getElementById(it)?.addClass("selectedRepertoireItem")
        }
    }
}

fun ContextMenu.addToPlaylistDropDown() {
    dropDown("Add to playlist", forDropDown = true).bind(repertoire.playlistCategories) { _ ->
        menu.maxHeight = 35.vh
        menu.width = 8.vw
        overflow = Overflow.VISIBLE
        menu.overflowY = Overflow.SCROLL
        menu.addCssClass("scroll-gold")
        menu.addCssClass("dropdown")

        div().bind(avariceSelected) {isAvarice ->
            val id = if(isAvarice) 0 else 1
            menu.overflow = Overflow.VISIBLE
            menu.overflowY = Overflow.SCROLL
            for (category in repertoire.playlistCategories.filter { it.configuredBot == id }) {
                val playlistsInCategory = repertoire.playlists.filter { it.categoryId == category.id }
                if(playlistsInCategory.isNotEmpty()) {
                    dropDown(category.name, forDropDown = true) {
                        textOverflow = TextOverflow.ELLIPSIS
                        whiteSpace = WhiteSpace.NOWRAP

                        for(playlist in playlistsInCategory) {
                            ddLink(playlist.name) {
                                textOverflow = TextOverflow.ELLIPSIS
                                whiteSpace = WhiteSpace.NOWRAP

                                onClick {
                                    sendMessage(
                                        AddItemsToPlaylistMessage(
                                            playlist,
                                            selectedItems.toList()
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

private fun playlistSongToSong(playlistSong: SharedPlaylistSong, song: SharedSong): SharedSong {
    song.order = playlistSong.order
    return song
}
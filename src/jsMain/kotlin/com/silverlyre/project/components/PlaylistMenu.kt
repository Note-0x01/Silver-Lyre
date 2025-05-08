package com.silverlyre.project.components

import com.silverlyre.messages.*
import com.silverlyre.project.*
import com.silverlyre.project.structures.PlaylistCategoryForm
import com.silverlyre.project.structures.PlaylistForm
import com.silverlyre.types.SharedPlaylistCategory
import com.silverlyre.types.SharedSong
import io.kvision.core.*
import io.kvision.dropdown.cmLink
import io.kvision.dropdown.contextMenu
import io.kvision.dropdown.ddLink
import io.kvision.dropdown.dropDown
import io.kvision.form.FormPanel
import io.kvision.form.check.radio
import io.kvision.form.form
import io.kvision.form.formPanel
import io.kvision.form.text.text
import io.kvision.html.*
import io.kvision.modal.Modal
import io.kvision.state.bind
import io.kvision.utils.*

fun Div.playlistMenu() {
    div{
        div {
            marginTop = 1.rem
            marginLeft = 1.rem
            display = Display.FLEX
            setStyle("width", "calc(${LEFT_COLUMN.asString()} - 2em)")
            justifyContent = JustifyContent.SPACEEVENLY

            input {
                id = "avariceRadio"
                name= "showPlaylist"
                type = InputType.RADIO
                checked = true
                position = Position.ABSOLUTE;
                opacity = 0.0;
                width = 0.px;
                height = 0.px;
            }.onClick {
                avariceSelected.setState(true)
                selectedPlaylistCategory.setState(null)
            }

            label {
                forId = "avariceRadio"
                image("/icons/avarice.png") {
                    height = 50.px
                    width = 50.px
                }
            }

            input {
                id = "edeaRadio"
                name= "showPlaylist"
                type = InputType.RADIO
                position = Position.ABSOLUTE;
                opacity = 0.0;
                width = 0.px;
                height = 0.px;
            }.onClick {
                avariceSelected.setState(false)
                selectedPlaylistCategory.setState(null)
            }

            label {
                forId = "edeaRadio"
                image("/icons/edea.png") {
                    height = 50.px
                    width = 50.px
                }
            }
        }
        div {
            marginTop = 0.5.rem
            marginLeft = 1.rem
            display = Display.FLEX
            flexDirection = FlexDirection.ROW
            flexGrow = 1
            h6("Playlist Categories") {
                marginRight = auto
            }
            image("/icons/plus-circle.svg") {
                addCssClass("icon-colour")
                float = PosFloat.RIGHT
                marginRight = 1.em
                onClick {
                    //Change to form
                    val modal = Modal("Create Playlist Category")
                    val form = this@playlistMenu.formPanel<PlaylistCategoryForm> {
                        text(label = "Name").bind(PlaylistCategoryForm::name, required = true).onEvent {
                            keypress = {
                                if (it.key == "Enter") {
                                    it.preventDefault()
                                    addCategoryFromForm(this@formPanel, modal)
                                }
                            }
                        }
                        radio(name = "bot", label = "Avarice").bind(PlaylistCategoryForm::avarice).setState(true)
                        radio(name = "bot", label = "Edea").bind(PlaylistCategoryForm::edea)
                    }

                    modal.add(form)

                    modal.addButton(Button("Close") {
                        onClick {
                            modal.hide()
                        }
                    })

                    modal.addButton(Button("Confirm") {
                        onClick {
                            addCategoryFromForm(form, modal)
                        }
                    })

                    modal.show()
                }
            }
        }
        div().bind(repertoire.playlistCategories) {
            marginLeft = 1.rem
            border = Border(2.px, BorderStyle.INSET, SilverLyreColours.SELECTED.colour)
            background = Background(color = SilverLyreColours.SECONDARY_BG.colour)
            height = 10.vh
            width = (LEFT_COLUMN.first.toInt() - 2).rem
            overflowY = Overflow.SCROLL
            addCssClass("scroll-gold")
            overflowX = Overflow.HIDDEN
            display = Display.FLEX
            flexDirection = FlexDirection.COLUMN

            div().bind(avariceSelected) { isAvarice ->
                val categories = it.filter { category ->
                    if(isAvarice) {
                        category.configuredBot == 0
                    } else {
                        category.configuredBot == 1
                    }
                }

                categories.forEach { category ->
                    div {
                        marginTop = 0.px
                        marginBottom = 0.px
                        paddingBottom = 0.px
                        h5(category.name) {
                            marginBottom = 0.px
                            paddingBottom = 0.px
                            overflow = Overflow.HIDDEN
                            textOverflow = TextOverflow.ELLIPSIS
                            whiteSpace = WhiteSpace.NOWRAP
                            height = 35.px
                            paddingLeft = 0.5.rem
                            if(selectedPlaylistCategory.getState() == category) {
                                color = SilverLyreColours.HOVER_TEXT.colour
                                border = Border(2.px, BorderStyle.SOLID, SilverLyreColours.TERTIARY_BG.colour)
                            }
                        }.onEvent {
                            mouseover = {
                                color = SilverLyreColours.HOVER_TEXT.colour
                            }

                            mouseleave = {
                                color = SilverLyreColours.TEXT.colour
                                background = Background(SilverLyreColours.SECONDARY_BG.colour)
                            }

                            mouseup = {mouse ->
                                if(mouse.button.toInt() == 0) {
                                    //procs an update
                                    avariceSelected.setState(avariceSelected.getState())
                                    selectedPlaylistCategory.setState(category)
                                } else if(mouse.button.toInt() == 2) {
                                    selectedItems.clear()
                                    selectedItems.add(category)
                                    openContextMenu()
                                }
                            }
                        }
                    }
                }
            }
        }
        div().bind(selectedPlaylistCategory) {
            marginTop = 1.rem
            it?.let {
                div {
                    marginLeft = 1.rem
                    display = Display.FLEX
                    flexDirection = FlexDirection.ROW
                    flexGrow = 1
                    h6("Playlists in ${it.name}") {
                        marginRight = auto
                    }
                    image("/icons/plus-circle.svg") {
                        addCssClass("icon-colour")
                        float = PosFloat.RIGHT
                        marginRight = 1.em
                        onClick {
                            //Change to form
                            val modal = Modal("Create Playlist")
                            val form = this@playlistMenu.formPanel<PlaylistForm> {
                                text(label = "Name").bind(PlaylistForm::name, required = true).onEvent {
                                    keypress = {key ->
                                        if (key.key == "Enter") {
                                            key.preventDefault()
                                            addPlaylistFromForm(this@formPanel, modal)
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
                                    addPlaylistFromForm(form, modal)
                                }
                            })

                            modal.show()
                        }
                    }
                }
                div().bind(repertoire.playlists) { playlists ->
                    background = Background(color = SilverLyreColours.SECONDARY_BG.colour)
                    marginLeft = 1.rem
                    border = Border(2.px, BorderStyle.INSET, SilverLyreColours.SELECTED.colour)
                    height = 25.vh
                    width = (LEFT_COLUMN.first.toInt() - 2).rem
                    overflowY = Overflow.SCROLL
                    addCssClass("scroll-gold")
                    overflowX = Overflow.HIDDEN
                    display = Display.FLEX
                    flexDirection = FlexDirection.COLUMN
                    playlists.filter { playlist -> playlist.categoryId == it.id }.forEach { playlist ->
                        div {
                            marginTop = 0.px
                            marginBottom = 0.px
                            paddingBottom = 0.px
                            h6(playlist.name) {
                                paddingTop = 0.1.rem
                                marginBottom = 0.px
                                paddingBottom = 0.px
                                overflow = Overflow.HIDDEN
                                textOverflow = TextOverflow.ELLIPSIS
                                whiteSpace = WhiteSpace.NOWRAP
                                height = 35.px
                                paddingLeft = 0.5.rem
                                if(selectedPlaylist.getState() == playlist) {
                                    color = SilverLyreColours.HOVER_TEXT.colour
                                    border = Border(2.px, BorderStyle.SOLID, SilverLyreColours.TERTIARY_BG.colour)
                                }
                            }.onEvent {
                                mouseup = {mouse ->
                                    if(mouse.button.toInt() == 0) {
                                        //procs an update
                                        avariceSelected.setState(avariceSelected.getState())
                                        selectedPlaylist.setState(playlist)
                                        currentView.setState(OpenRepertoire.PLAYLIST)
                                    } else if(mouse.button.toInt() == 2) {
                                        selectedItems.clear()
                                        selectedItems.add(playlist)
                                        openContextMenu()
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

private fun Div.openContextMenu() {
    contextMenu {
        overflow = Overflow.VISIBLE
        addCssClass("dropright")
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
                            .onEvent { ->
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
                    sendMessage(
                        RenameItemsMessage(
                            finalItems
                        )
                    )
                    selectedItems.clear()
                    modal.hide()
                }
            })

            modal.show()
        }
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

private fun addCategoryFromForm(
    form: FormPanel<PlaylistCategoryForm>,
    modal: Modal
) {
    val formContents = form.getData()
    if (formContents.name.isNotEmpty() && formContents.name.isNotBlank()) {
        val bot = if (formContents.avarice) {
            botsToManage[0]
        } else {
            botsToManage[1]
        }

        sendMessage(
            AddPlaylistCategoryMessage(
                formContents.name,
                bot
            )
        )
        modal.hide()
    }
}

private fun addPlaylistFromForm(
    form: FormPanel<PlaylistForm>,
    modal: Modal
) {
    val formContents = form.getData()
    if (formContents.name.isNotEmpty() && formContents.name.isNotBlank() && selectedPlaylistCategory.getState() != null) {
        sendMessage(
            AddPlaylistMessage(
                formContents.name,
                selectedPlaylistCategory.getState()!!
            )
        )
        modal.hide()
    }
}
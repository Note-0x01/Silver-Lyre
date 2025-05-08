package com.silverlyre.project.structures

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistCategoryForm(
    val name: String,
    val avarice: Boolean = true,
    val edea: Boolean
)

@Serializable
data class PlaylistForm(
    val name: String
)
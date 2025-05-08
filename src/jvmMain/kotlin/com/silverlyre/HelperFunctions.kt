package com.silverlyre

import dev.kord.core.entity.User
import dev.kord.rest.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

fun getAlbumDirectoryFromName(album: String): File {
    val albumClean = album.filter { it.isLetterOrDigit() }.trim()
    return File("$musicRootPath/${albumClean}")
}

suspend fun readImage(user: User): BufferedImage? {
    try {
        return ImageIO.read(ByteArrayInputStream(user.avatar?.getImage(Image.Format.PNG, Image.Size.Size256)?.data))
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

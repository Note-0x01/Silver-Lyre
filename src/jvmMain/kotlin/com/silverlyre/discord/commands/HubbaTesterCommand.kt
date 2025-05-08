package com.silverlyre.discord.commands

import com.silverlyre.readImage
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.Image
import dev.kord.rest.builder.interaction.user
import io.ktor.client.request.forms.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.random.Random


private data class HubbaUser(public val user: User, public val isBard: Boolean) { //add hubbarelationship data class?
    public var love: Int = -1

    public fun print() {
        println("${user.username} - $isBard - $love")
    }
}
class HubbaTesterCommand(private val self: User, private val secondary: User?) : Command {
    override val name: String = "hubbatester"
    override val description: String = "Rates the synergy and relationship between two characters in the ~army~ discord server."
    private val imageWidth = 1664
    private val imageHeight = 958

    override suspend fun setup(kord: Kord) {
        kord.createGlobalChatInputCommand(
            name,
            description,
        ) {
            user("first_user", "The left user to test") {
                required = true
            }
            user("second_user", "The right user to test") {
                required = true
            }
        }

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            if(interaction.command.rootName == name) {
                val response = interaction.deferPublicResponse()
                val command = interaction.command
                val first = command.users["first_user"]!!
                val second = command.users["second_user"]!!

                executeCommand(first, second, response)
            }
        }
    }

    private suspend fun executeCommand(
        first: User,
        second: User,
        response: DeferredPublicMessageInteractionResponseBehavior
    ) {
        val users = ArrayList<HubbaUser>()
        if (first == self || first == secondary) {
            users.add(HubbaUser(first, true))
        } else {
            users.add(HubbaUser(first, false))
        }

        if (second == self || second == secondary) {
            users.add(HubbaUser(second, true))
        } else {
            users.add(HubbaUser(second, false))
        }

        if (users.size == 1)
            users.add(users[0])

        if(users.all { it.isBard }) { // If Avarice and Edea are match, or any with themselves, max out relationship
            users.forEach {
                it.love = 2
            }
        } else if (users.any { it.isBard }) { // Avarice and Edea only like each other and tolerate others :^)
            users.forEach {
                if(it.isBard)
                    it.love = 0
                else
                    it.love = Random.nextInt(3)
            }
        } else {
            users.forEach {
                it.love = Random.nextInt(3)
            }
        }

        val hubba = BufferedImage(imageWidth, imageHeight, TYPE_INT_ARGB)
        val canvas = hubba.createGraphics()

        addImages(users, canvas)
        addFeelings(users, canvas)

        val fileLocation = Files.createTempFile("hubba", ".png").toFile()
        try {
            ImageIO.write(hubba, "png", fileLocation)
        } catch (e: IOException) {
            e.printStack()
        }

        response.respond {
            addFile("${first.username}&${second.username}.png", ChannelProvider{ fileLocation.readChannel() } )
        }
    }

    private suspend fun addImages(
        users: ArrayList<HubbaUser>,
        canvas: Graphics2D
    ) {
        val firstAvatar = readImage(users[0].user)
        val secondAvatar = readImage(users[1].user)
        val loveSum = users.sumOf { it.love }
        val bg = ImageIO.read(javaClass.getResource("/hubbatester/${determineCompatibility(loveSum)}.png"))
        val topArrow = ImageIO.read(javaClass.getResource("/hubbatester/${users[0].love}.png"))
        val bottomArrow = ImageIO.read(javaClass.getResource("/hubbatester/${users[1].love}.png"))

        canvas.drawImage(bg, 0, 0, 1664, 958, null)
        canvas.drawImage(firstAvatar, 200, 216, 330, 330, null)
        canvas.drawImage(secondAvatar, 1133, 216, 330, 330, null)

        canvas.drawImage(topArrow, 609, 300, 447, 56, null)
        canvas.drawImage(bottomArrow, 609 + 447, 400, -447, 56, null)
    }

    private fun addFeelings(
        users: ArrayList<HubbaUser>,
        canvas: Graphics2D,
    ) {
        val jsonFileString = File("hubba.json").bufferedReader().use { it.readText() }
        val map = Json.decodeFromString<HashMap<String, ArrayList<String>>>(jsonFileString)
        addArrowText(canvas, users[0].love, map, 281)
        addArrowText(canvas, users[1].love, map, 521)

        addRelationshipText(canvas, map, users[0].love, users[1].love)
    }

    private fun addRelationshipText(
        canvas: Graphics2D,
        map: HashMap<String, ArrayList<String>>,
        firstLove: Int,
        secondLove: Int
    ) {
        val text = map[getRelationship(firstLove, secondLove)]!![Random.nextInt(map[getRelationship(firstLove, secondLove)]!!.size)]

        canvas.font = Font("Sans-serif", Font.BOLD, 55)
        canvas.color = Color.decode("#4a3318")
        canvas.drawString(
            text,
            500,
            821
        )
    }

    private fun getRelationship(firstLove: Int, secondLove: Int): String {
        return if (numbersOr(firstLove, secondLove, 2, 2)) {
            "mutualBest"
        } else if (numbersOr(firstLove, secondLove, 1, 2)) {
            "bestGood"
        } else if (numbersOr(firstLove, secondLove, 0, 2)) {
            "bestBad"
        } else if (numbersOr(firstLove, secondLove, 1, 1)) {
            "mutualGood"
        } else if (numbersOr(firstLove, secondLove, 0, 1)) {
            "goodBad"
        }  else if (numbersOr(firstLove, secondLove, 0, 0)) {
            "mutualBad"
        } else {
            "mutualGood"
        }
    }

    private fun numbersOr(first: Int, second: Int, x: Int, y: Int): Boolean {
        return first == x && second == y || first == y && second == x
    }

    private fun addArrowText(
        canvas: Graphics2D,
        loveLevel: Int,
        map: HashMap<String, ArrayList<String>>,
        height: Int
    ) {
        var colour = ""
        var text = ""
        when (loveLevel) {
            0 -> {
                colour = "#1d1e27"
                text = map["badFeeling"]!![Random.nextInt(map["badFeeling"]!!.size)]
            }
            1 -> {
                colour = "#145106"
                text = map["goodFeeling"]!![Random.nextInt(map["goodFeeling"]!!.size)]
            }
            2 -> {
                colour = "#851c29"
                text = map["bestFeeling"]!![Random.nextInt(map["bestFeeling"]!!.size)]
            }
        }
        canvas.font = Font("Sans-serif", Font.PLAIN, 60)
        canvas.color = Color.decode(colour)
        canvas.drawString(
            text,
            (imageWidth / 2) - canvas.fontMetrics.stringWidth(text) / 2,
            height
        )
    }

    private fun determineCompatibility(loveSum: Int): String {
        return when (loveSum) {
            4 -> {
                "Good"
            }
            0 -> {
                "Bad"
            }
            else -> {
                "Mutual"
            }
        }
    }
}
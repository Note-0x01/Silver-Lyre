package com.silverlyre.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.silverlyre.discord.commands.Command
import com.silverlyre.discord.commands.HubbaTesterCommand
import com.silverlyre.discord.commands.JoinCommand
import com.silverlyre.music.MusicPlayer
import com.silverlyre.readImage
import com.silverlyre.types.DiscordChannel
import com.silverlyre.types.DiscordGuild
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.connect
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.voice.AudioFrame
import dev.kord.voice.VoiceConnection
import io.ktor.client.request.forms.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.IOException
import java.nio.file.Files
import javax.imageio.ImageIO

class BardBot(private val botToken: String, private val isSecondary: Boolean = false): Thread() {
    val bardState = if(isSecondary)
        "Edea"
    else
        "Avarice"

    private val logger: Logger = LoggerFactory.getLogger(bardState)
    private var kord: Kord? = null;
    var channel: VoiceChannel? = null
    var musicPlayer: MusicPlayer? = null
    var audioPlayer: AudioPlayer? = null

    @OptIn(KordVoice::class)
    var connection: VoiceConnection? = null

    override fun run() {
        logger.info("$bardState thread starting.")
        runBlocking {
            launch {
                startAndLogin(botToken)
            }
        }
    }

    fun getGuildsAndChannels(id: Int): List<DiscordGuild> {
        return runBlocking {
            val guilds = mutableListOf<DiscordGuild>()

            kord!!.guilds.toList().sorted().forEach {
                val channels = mutableListOf<DiscordChannel>()
                it.channels.toList().sorted().forEach { channel ->
                    if(channel is VoiceChannel) {
                        channels.add(DiscordChannel(
                            channel.id.value,
                            channel.name
                        ))
                    }
                }
                guilds.add(DiscordGuild( id ,it.id.value, it.name, channels))
            }

            return@runBlocking guilds
        }
    }
    @OptIn(PrivilegedIntent::class)
    private suspend fun startAndLogin(botToken: String) {
        logger.info("$bardState logged in.")

        kord = Kord(botToken);

        if(!isSecondary) {
            configureCommands()
            configureEvents()
            logger.info("$bardState finished configuring commands and events.")
        }

        kord!!.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
            intents += Intent.GuildVoiceStates
            intents += Intent.GuildMembers
        }
    }

    @OptIn(KordVoice::class)
    fun joinChannel(channelId: ULong) {
        runBlocking {
            channel = kord!!.getChannel(Snowflake(channelId))!!.asChannelOf<VoiceChannel>()
            connection?.shutdown()
            connection = channel?.connect {
                audioProvider { AudioFrame.fromData(audioPlayer?.provide()?.data)}
            }
        }
    }

    private suspend fun configureCommands() {
        logger.info("Configuring commands.")
        val commandsList = listOf(
            HubbaTesterCommand(
                this.kord!!.getSelf(),
                this.kord!!.getUser(Snowflake("919794007222124544"))
            ),
            JoinCommand(this)
        )

        for(command in commandsList) {
            command.setup(kord!!)
            logger.info("Registering command: ${command.name}")
        }
    }

    private fun configureEvents() {
        logger.info("Configuring events.")
        this.kord!!.on<MemberJoinEvent> {
            if(this.getGuild().id.value.toLong() == 1006343347247530054) {
                val joinImage = BufferedImage(1180, 723, TYPE_INT_ARGB)
                val canvas = joinImage.createGraphics()
                val avatar = readImage(this.member.asUser())
                val name = this.member.effectiveName
                var size = 50
                if(name.length >= 25)
                    size = 40

                val bg = ImageIO.read(javaClass.getResource("/joinart/joinTemplate.png"))
                canvas.drawImage(bg, 0, 0, 1180, 723, null)
                canvas.drawImage(avatar, 124, 103, 344, 344, null)
                canvas.font = Font("Sans-serif", Font.BOLD, size)
                canvas.color = Color.decode("#CC822D")

                canvas.drawString(
                    name,
                    299 - canvas.fontMetrics.stringWidth(name) / 2,
                    510
                )

                val fileLocation = Files.createTempFile("join_${this.member.username}", ".png").toFile()
                try {
                    ImageIO.write(joinImage, "png", fileLocation)
                } catch (e: IOException) {
                    e.printStack()
                }

                this.getGuild().getSystemChannel()!!.createMessage {
                    addFile("join.png", ChannelProvider{ fileLocation.readChannel() } )
                }
            }
        }
    }

}
package com.silverlyre.discord.commands

import com.silverlyre.discord.BardBot
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.ChannelType
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.connect
import dev.kord.core.entity.channel.VoiceChannel
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.channel
import dev.kord.voice.AudioFrame

class JoinCommand(private val bard: BardBot): Command{
    override val name: String = "join"
    override val description: String = "join"
    @OptIn(KordVoice::class)
    override suspend fun setup(kord: Kord) {
        kord.createGlobalChatInputCommand(
            name,
            description,
        ) {
            channel("channel", "Voice channel to connect to.") {
                required = true
                channelTypes = listOf(ChannelType.GuildVoice)
            }
        }

        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            if(interaction.command.rootName == name) {
                val thisCommand = interaction.command
                val channel = thisCommand.channels["channel"]!!.asChannelOf<VoiceChannel>()

                bard.channel = channel
                bard.connection?.disconnect()
                bard.connection = bard.channel?.connect {
                    audioProvider { AudioFrame.fromData(bard.audioPlayer?.provide()?.data)}
                }
            }
        }
    }
}
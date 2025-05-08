package com.silverlyre.discord.commands

import dev.kord.core.Kord

interface Command {
    val name: String
    val description: String

    suspend fun setup(kord: Kord)
}
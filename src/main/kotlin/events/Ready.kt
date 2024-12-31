@file:Suppress("SpellCheckingInspection", "SENSELESS_COMPARISON")

package events

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import utils.NekoCLIApi
import java.awt.Color
import java.time.Duration
import java.time.Instant

class Ready : EventListener {

    private val api = NekoCLIApi()

    override fun onEvent(event: GenericEvent) {
        if (event !is ReadyEvent) return

        try {
            event.jda.presence.setStatus(OnlineStatus.DO_NOT_DISTURB)
            val startTime = Instant.now()
            api.autoActivity(event.jda)
            api.preventLag()

            val commands = try {
                api.getAllCommands(event.jda)
            } catch (e: Exception) {
                AnsiConsole.systemInstall()
                println(ansi().fgBrightRed().a("[Error]").reset().a(" Failed to retrieve commands: ${e.message}"))
                emptyList()
            }

            val commandNames = commands.map { it.name }
            val endTime = Instant.now()
            val startupTime = Duration.between(startTime, endTime).toMillis()

            AnsiConsole.systemInstall()
            println(ansi().fgBrightBlue().a("\n[Startup Complete] Commands Registered:\n").reset())

            val tableHeader = "| Command Name       | Description                 |"
            val tableDivider = "|--------------------|-----------------------------|"
            println(ansi().fgCyan().a(tableHeader).reset())
            println(ansi().fgCyan().a(tableDivider).reset())

            commands.forEach {
                val name = it.name.padEnd(18, ' ')
                val description = it.description.padEnd(29, ' ')
                println(ansi().fgBrightGreen().a("| $name | $description |").reset())
            }

            println(ansi().fgCyan().a(tableDivider).reset())
            println(ansi().fgBrightBlue().a("\n[Startup Complete] Startup Time: ${startupTime}ms").reset())
            println(ansi().fgBrightBlue().a("[Startup Complete] Logged In As: ${event.jda.selfUser.name}").reset())

            val embed = EmbedBuilder()
                .setTitle("🤖 Bot Status: Online")
                .setDescription("The bot is now online and fully operational.")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .addField("⏱️ **Startup Time**", "`${startupTime}ms`", false)
                .addField("👤 **Logged In As**", "`${event.jda.selfUser.name}`", false)
                .addField("🛠️ **Registered Commands**", commandNames.joinToString(", ") { "`$it`" }.ifEmpty { "No commands registered" }, false)
                .addField("🔔 **Status**", "`Online (Do Not Disturb)`", false)
                .setFooter("Neko-CLI-Worker | Ready", event.jda.selfUser.avatarUrl)
                .setTimestamp(Instant.now())
                .build()

            val logChannelId = api.getConfig("LOGCHANNELID")
            if (logChannelId == null) {
                println(ansi().fgBrightRed().a("[Critical Error] Log channel ID is null!").reset())
                return
            }

            val logChannel = event.jda.getTextChannelById(logChannelId)

            if (logChannel != null) {
                println(ansi().fgBrightGreen().a("[Success] Log channel found. Sending embed...").reset())
                logChannel.sendMessageEmbeds(embed).queue()
            } else {
                println(ansi().fgYellow().a("[Warning] Log channel not found for ID: $logChannelId").reset())
            }

        } catch (e: Exception) {
            println(ansi().fgBrightRed().a("[Critical Error] Exception in Ready listener: ${e.message}").reset())
            e.printStackTrace()
        }
    }
}


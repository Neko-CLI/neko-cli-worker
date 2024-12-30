@file:Suppress("SpellCheckingInspection")

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
                println("[Error] Failed to retrieve commands: ${e.message}")
                emptyList()
            }

            val commandNames = commands.joinToString(", ") { it.name }
            val endTime = Instant.now()
            val startupTime = Duration.between(startTime, endTime).toMillis()
            AnsiConsole.systemInstall()
            println("${ansi().fgBrightBlue()}[Startup Complete]${ansi().reset()} Startup Time: ${startupTime}ms")
            println("${ansi().fgBrightBlue()}[Startup Complete]${ansi().reset()} Logged In As: ${event.jda.selfUser.name}")
            println("${ansi().fgBrightBlue()}[Startup Complete]${ansi().reset()} Registered Commands: ${if (commandNames.isEmpty()) "No commands registered" else commandNames}")
            val embed = EmbedBuilder()
                .setTitle("Bot Online")
                .setDescription("The bot is now online and ready to operate.")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .addField("Startup Time", "${startupTime}ms", false)
                .addField("Logged In As", event.jda.selfUser.name, false)
                .addField("Registered Commands", commandNames.ifEmpty { "No commands registered" }, false)
                .addField("Status", "Online and set to Do Not Disturb", false)
                .setFooter("Neko-CLI-Worker", event.jda.selfUser.avatarUrl)
                .setTimestamp(Instant.now())
                .build()

            val logChannelId = api.getConfig("LOGCHANNELID")
            val logChannel = event.jda.getTextChannelById(logChannelId)

            if (logChannel != null) {
                logChannel.sendMessageEmbeds(embed).queue()
            } else {
                println("[Warning] Log channel not found for ID: $logChannelId")
            }

        } catch (e: Exception) {
            println("[Critical Error] Exception in Ready listener: ${e.message}")
            e.printStackTrace()
        }
    }
}


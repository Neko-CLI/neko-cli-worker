package events

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.fusesource.jansi.AnsiConsole
import net.dv8tion.jda.api.EmbedBuilder
import org.fusesource.jansi.Ansi.ansi
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant
import java.time.Duration

class Ready : EventListener {

    private val api = NekoCLIApi()

    override fun onEvent(event: GenericEvent) {
        if (event !is ReadyEvent) return
        event.jda.presence.setStatus(OnlineStatus.DO_NOT_DISTURB)
        val startTime = Instant.now()
        api.autoActivity(event.jda)
        api.preventLag()
        val commands = api.getAllCommands(event.jda)
        val commandNames = commands.joinToString(", ") { it.name }
        val endTime = Instant.now()
        val startupTime = Duration.between(startTime, endTime).toMillis()
        AnsiConsole.systemInstall()
        println("${ansi().fgBrightGreen()}[Startup Complete]${ansi().reset()} ${ansi().fgBrightBlue()}Startup Time:${ansi().reset()} ${startupTime}ms")
        println("${ansi().fgBrightGreen()}[Startup Complete]${ansi().reset()} ${ansi().fgBrightBlue()}Logged In As:${ansi().reset()} ${event.jda.selfUser.asTag}")
        println("${ansi().fgBrightGreen()}[Startup Complete]${ansi().reset()} ${ansi().fgBrightBlue()}Registered Commands:${ansi().reset()} ${if (commandNames.isEmpty()) "No commands registered" else commandNames}")
        val embed = EmbedBuilder()
            .setTitle("Bot Online")
            .setDescription("The bot is now online and ready to operate.")
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .addField("Startup Time", "${startupTime}ms", false)
            .addField("Logged In As", event.jda.selfUser.asTag, false)
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
            println("${ansi().fgBrightRed()}[Error]${ansi().reset()} Log channel with ID $logChannelId not found.")
        }

        FactOfTheDay(event.jda)
    }
}

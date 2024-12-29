package events

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.events.ExceptionEvent
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.utils.data.DataObject
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import utils.NekoCLIApi
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WorkerErrors() : EventListener {

    private val api = NekoCLIApi()

    override fun onEvent(event: GenericEvent) {
        if (event is ExceptionEvent) {
            handleException(event)
        } else if (event is WarningEvent) {
            handleWarning(event)
        }
    }

    private fun handleException(event: ExceptionEvent) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        event.cause.printStackTrace(pw)
        val stackTrace = "```java\n${sw.toString().take(1900)}\n```"
        AnsiConsole.systemInstall()
        println(
            Ansi.ansi().fgBrightBlue().a("[").reset().a("NekoCLIWorker").fgBrightBlue().a("]").reset()
                .fgBrightRed().a(" Error").reset()
                .fgBrightBlue().a(" | ").reset()
                .fgBrightCyan().a("Message: ${event.cause.message}").reset()
                .fgBrightBlue().a(" | ").reset()
                .fgBrightCyan().a("Type: ${event.cause.javaClass.name}").reset()
        )
        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
        val embed = EmbedBuilder()
            .setTitle("🚨 Worker Error Report")
            .setDescription("An exception has occurred in the system.")
            .setColor(Color.RED)
            .addField("**Error Message**", event.cause.message ?: "No message available", false)
            .addField("**Error Type**", event.cause.javaClass.name, false)
            .addField("**Stack Trace**", stackTrace, false)
            .addField("**Occurred At**", timestamp, false)
            .setFooter("NekoCLIWorker Error Handler", event.jda.selfUser.avatarUrl)
            .setTimestamp(Instant.now())
            .build()

        sendToLogChannel(event.jda, embed, "Error")
    }

    private fun handleWarning(event: WarningEvent) {
        AnsiConsole.systemInstall()
        println(
            Ansi.ansi().fgBrightBlue().a("[").reset().a("NekoCLIWorker").fgBrightBlue().a("]").reset()
                .fgYellow().a(" Warning").reset()
                .fgBrightBlue().a(" | ").reset()
                .fgBrightCyan().a("Message: ${event.message}").reset()
        )
        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
        val embed = EmbedBuilder()
            .setTitle("⚠️ Worker Warning Report")
            .setDescription("A warning has been issued.")
            .setColor(Color.YELLOW)
            .addField("**Warning Message**", event.message, false)
            .addField("**Occurred At**", timestamp, false)
            .setFooter("NekoCLIWorker Warning Handler", event.jda.selfUser.avatarUrl)
            .setTimestamp(Instant.now())
            .build()

        sendToLogChannel(event.jda, embed, "Warning")
    }
    private fun sendToLogChannel(jda: JDA, embed: net.dv8tion.jda.api.entities.MessageEmbed, logType: String) {
        val logChannelId = api.getConfig("LOGCHANNELID")
        val logChannel = jda.getTextChannelById(logChannelId)

        if (logChannel != null) {
            logChannel.sendMessageEmbeds(embed).queue()
        } else {
            println(
                Ansi.ansi().fgBrightRed().a("[Error]").reset().a(" $logType log channel not found.").reset()
            )
        }
    }
}

class WarningEvent(val message: String, private val jda: JDA) : GenericEvent {
    override fun getJDA() = jda
    override fun getResponseNumber() = 0L
    override fun getRawData(): DataObject? = null
}

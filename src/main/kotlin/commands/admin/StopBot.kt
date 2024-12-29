package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class StopBot : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "stopbot") return

        val password: String? = event.getOption("password", OptionMapping::getAsString)

        if (event.guild?.id != api.getConfig("GUILDID")) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Error")
                    .setDescription("You are not in the correct server to use this command.")
                    .setImage(api.getConfig("SERVERIMAGE"))
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (!api.isUnStackss(event.member?.id.toString())) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Permission Denied")
                    .setDescription("You are not authorized to use this command.")
                    .setImage(api.getConfig("SERVERIMAGE"))
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (password != api.getConfig("PASSWORD")) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Incorrect Password")
                    .setDescription("The password provided is incorrect. Please try again.")
                    .setImage(api.getConfig("SERVERIMAGE"))
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
        val shuttingDownEmbed = EmbedBuilder()
            .setTitle("🛑 Shutting Down")
            .setDescription("The bot is shutting down as per the request of ${event.member?.asMention}.")
            .setImage(api.getConfig("SERVERIMAGE"))
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
            .addField("Requested By", event.member?.asMention ?: "Unknown", true)
            .addField("Timestamp", timestamp, true)
            .setTimestamp(Instant.now())
            .build()

        event.hook.sendMessageEmbeds(shuttingDownEmbed).queue {
            api.stopTheBot(event.jda)
        }

        println("[Shutdown Command] Bot shutdown initiated by ${event.member?.user?.name}#${event.member?.user?.discriminator}")
    }
}

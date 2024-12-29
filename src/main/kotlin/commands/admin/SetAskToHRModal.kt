package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant

class SetAskToHRModal : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "setasktohrmodal") return

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

        event.deferReply().queue()

        val initialEmbed = EmbedBuilder()
            .setTitle("🛠️ Setting Up Ask to HR Modal")
            .setDescription("We are configuring the HR question modal... Please wait.")
            .setImage(api.getConfig("SERVERIMAGE"))
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
            .setTimestamp(Instant.now())
            .build()

        event.hook.sendMessageEmbeds(initialEmbed).queue {
            val successEmbed = EmbedBuilder()
                .setTitle("🙋‍ Ask HR Your Questions")
                .setDescription("Click the button below to ask your question to the HR team.")
                .setImage(api.getConfig("SERVERIMAGE"))
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                .addField("📋 Instructions", "1. Click the button below.\n2. Fill out the modal form.\n3. Submit your question to HR.", false)
                .setTimestamp(Instant.now())
                .build()

            event.hook.editOriginalEmbeds(successEmbed).setActionRow(api.sendQuestionToHR()).queue()
        }
    }
}

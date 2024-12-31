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
                    .setTitle("🚫 **Access Denied**")
                    .setDescription(
                        "❌ You are not in the correct server to use this command.\n\n" +
                                "📌 **Tip:** This command is restricted to specific servers. Please ensure you are in the authorized server."
                    )
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
            .setTitle("🛠️ **Setting Up Ask to HR Modal**")
            .setDescription(
                "We are configuring the **HR question modal**... Please wait a moment.\n\n" +
                        "🔧 **Status:** In Progress"
            )
            .setImage(api.getConfig("SERVERIMAGE"))
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
            .setTimestamp(Instant.now())
            .build()

        event.hook.sendMessageEmbeds(initialEmbed).queue {
            val successEmbed = EmbedBuilder()
                .setTitle("🙋‍ **Ask HR Your Questions**")
                .setDescription(
                    "The **Ask to HR Modal** has been successfully configured.\n\n" +
                            "📋 **Instructions:**\n" +
                            "1. Click the button below.\n" +
                            "2. Fill out the modal form with your question.\n" +
                            "3. Submit the form to reach the HR team."
                )
                .setImage(api.getConfig("SERVERIMAGE"))
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                .addField(
                    "🔎 **Need Help?**",
                    "For more details or assistance, please refer to [our documentation](${api.getConfig("WEBSITE")}).",
                    false
                )
                .setFooter("HR Modal Setup Completed", event.jda.selfUser.avatarUrl)
                .setTimestamp(Instant.now())
                .build()

            event.hook.editOriginalEmbeds(successEmbed).setActionRow(api.sendQuestionToHR()).queue()
        }
    }
}

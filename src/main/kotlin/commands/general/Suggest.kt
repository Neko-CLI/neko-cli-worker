@file:Suppress("SpellCheckingInspection", "USELESS_ELVIS")

package commands.general

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Suggest : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "suggest") return

        val suggestionInput = TextInput.create("suggestion_detail", "💡 Your Suggestion", TextInputStyle.PARAGRAPH)
            .setPlaceholder("📝 Share your brilliant idea or feedback here...")
            .setRequired(true)
            .setMaxLength(1000)
            .build()

        val modal = Modal.create("suggestion_modal", "✨ Submit Your Suggestion")
            .addActionRow(suggestionInput)
            .build()

        event.replyModal(modal).queue()
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != "suggestion_modal") return

        val suggestion = event.getValue("suggestion_detail")?.asString

        if (suggestion.isNullOrBlank()) {
            event.reply("❌ **Your suggestion cannot be empty. Please provide some details.**")
                .setEphemeral(true)
                .queue()
            return
        }

        val suggestionChannel = event.jda.getTextChannelById(api.getConfig("SUGGESTIONSCHANNELID"))
        if (suggestionChannel == null) {
            val errorMessage = if (event.isFromGuild) {
                "❌ **Suggestion channel not found in this server. Please contact an administrator.**"
            } else {
                "❌ **Suggestion channel is not accessible in direct messages. Please contact an administrator.**"
            }
            event.reply(errorMessage).setEphemeral(true).queue()
            return
        }

        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
        val embed = EmbedBuilder()
            .setTitle("💡 **New Suggestion!** 📝")
            .setDescription(
                "🔍 **Suggestion Details:**\n\n```$suggestion```"
            )
            .addField("👤 **Suggested by**", "${event.user.asTag} \\`${event.user.id}\\`", false)
            .addField("📅 **Submitted on**", timestamp, false)
            .setThumbnail(event.user.effectiveAvatarUrl ?: event.jda.selfUser.effectiveAvatarUrl)
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .setFooter("Thank you for your valuable feedback!", event.jda.selfUser.effectiveAvatarUrl)
            .build()

        suggestionChannel.sendMessageEmbeds(embed).queue {
            event.reply("✅ **Thank you! Your suggestion has been submitted successfully.** 🎉")
                .setEphemeral(true)
                .queue()
        }
    }
}

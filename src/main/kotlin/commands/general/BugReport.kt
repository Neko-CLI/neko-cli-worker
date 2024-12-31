@file:Suppress("SpellCheckingInspection")

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

class BugReport : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "bugreport") return

        val bugDescriptionInput = TextInput.create("bug_description", "\uD83D\uDC1E Bug Description", TextInputStyle.PARAGRAPH)
            .setPlaceholder("\uD83D\uDCDD Describe the bug you encountered...")
            .setRequired(true)
            .setMaxLength(1000)
            .build()

        val modal = Modal.create("bugreport_modal", "\uD83D\uDEA8 Report a Bug")
            .addActionRow(bugDescriptionInput)
            .build()

        event.replyModal(modal).queue()
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != "bugreport_modal") return

        val bugDescription = event.getValue("bug_description")?.asString

        if (bugDescription.isNullOrBlank()) {
            event.reply("❌ Bug description cannot be empty. Please provide details.").setEphemeral(true).queue()
            return
        }

        val bugReportChannel = event.jda.getTextChannelById(api.getConfig("BUGREPORTSCHANNELID"))
        if (bugReportChannel == null) {
            if (event.isFromGuild) {
                event.reply("❌ Bug report channel not found in this server. Please contact an administrator.").setEphemeral(true).queue()
            } else {
                event.reply("❌ Bug report channel is not accessible in direct messages. Please contact an administrator.").setEphemeral(true).queue()
            }
            return
        }

        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
        val embed = EmbedBuilder()
            .setTitle("\uD83D\uDC1E New Bug Report! ⚠️")
            .setDescription("\uD83D\uDD0D **Bug Details:**\n$bugDescription")
            .addField("\uD83D\uDC64 Reported by", event.user.name, false)
            .setFooter("\uD83D\uDCC5 Reported on $timestamp", event.user.effectiveAvatarUrl)
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .build()

        bugReportChannel.sendMessageEmbeds(embed).queue {
            event.reply("✅ **Thank you!** Your bug report has been submitted successfully. \uD83D\uDDA0️")
                .setEphemeral(true)
                .queue()
        }
    }
}

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

        val bugDescriptionInput = TextInput.create("bug_description", "🐞 Bug Description", TextInputStyle.PARAGRAPH)
            .setPlaceholder("📝 Describe the bug you encountered...")
            .setRequired(true)
            .setMaxLength(1000)
            .build()

        val modal = Modal.create("bugreport_modal", "🚨 Report a Bug")
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
            event.reply("❌ Bug report channel not found. Please contact an administrator.").setEphemeral(true).queue()
            return
        }

        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
        val embed = EmbedBuilder()
            .setTitle("🐞 New Bug Report! ⚠️")
            .setDescription("🔍 **Bug Details:**\n$bugDescription")
            .addField("👤 Reported by", event.user.asTag, false)
            .setFooter("📅 Reported on $timestamp", event.user.effectiveAvatarUrl)
            .setColor(Color.RED)
            .build()

        bugReportChannel.sendMessageEmbeds(embed).queue {
            event.reply("✅ **Thank you!** Your bug report has been submitted successfully. 🛠️")
                .setEphemeral(true)
                .queue()
        }
    }
}

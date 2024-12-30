@file:Suppress("SpellCheckingInspection")

package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TicketForums(
    private val api: NekoCLIApi
) : ListenerAdapter() {
    private val jda = api.getJdaInstance()
    private val forumChannelId = api.getConfig("FORUMCHANNELID")
    private val staffRoleId = api.getConfig("FTTACCESSID")
    private val openTickets = mutableMapOf<String, String>()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "setticketforum") return

        val embed = EmbedBuilder()
            .setTitle("🎫 Open a Ticket")
            .setDescription(
                "If you need assistance, click the **Open Ticket** button below. " +
                        "A public post will be created in the forum. If you need private assistance, please ask in <#1322533274953973821>."
            )
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .setThumbnail(event.jda.selfUser.effectiveAvatarUrl)
            .setFooter("Ticket System", event.jda.selfUser.effectiveAvatarUrl)
            .setTimestamp(Instant.now())
            .build()

        val openTicketButton = Button.primary("open-ticket", "🎟️ Open Ticket")

        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("✅ Success")
                .setDescription("The ticket system has been configured in this channel.")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .build()
        ).setEphemeral(true).queue()

        event.channel.sendMessageEmbeds(embed).setActionRow(openTicketButton).queue()
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when (event.componentId) {
            "open-ticket" -> handleOpenTicket(event)
            "close-ticket" -> handleCloseTicket(event)
            "delete-ticket" -> handleDeleteTicket(event)
        }
    }

    private fun handleOpenTicket(event: ButtonInteractionEvent) {
        val userId = event.user.id

        if (openTickets.containsKey(userId)) {
            val existingThreadId = openTickets[userId]
            val threadUrl = jda.getThreadChannelById(existingThreadId!!)?.jumpUrl
            event.reply("❌ You already have an open ticket. Please resolve it before opening a new one. [View your ticket]($threadUrl)")
                .setEphemeral(true)
                .queue()
            return
        }

        val guild = event.guild
        val forumChannel = guild?.getChannelById(ForumChannel::class.java, forumChannelId)

        if (forumChannel == null) {
            event.reply("❌ Ticket forum not found. Please contact an administrator.")
                .setEphemeral(true)
                .queue()
            return
        }

        val timestamp = Instant.now()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy-HH:mm"))
        val postName = "Ticket-${event.user.name}-$timestamp"

        forumChannel.createForumPost(
            postName,
            MessageCreateBuilder().setContent("Ticket created by ${event.user.asMention}").build()
        ).queue { post ->
            val thread = post.threadChannel

            openTickets[userId] = thread.id

            notifyUser(event, thread.jumpUrl)
            addActionButtons(thread)
        }
    }

    private fun notifyUser(event: ButtonInteractionEvent, ticketUrl: String) {
        val embed = EmbedBuilder()
            .setTitle("🎟️ Ticket Created")
            .setDescription("Your ticket has been created successfully. Staff will assist you shortly.")
            .addField("📎 Ticket Link", "[Click here to access your ticket]($ticketUrl)", false)
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .setFooter("Ticket System", event.jda.selfUser.effectiveAvatarUrl)
            .setTimestamp(Instant.now())
            .build()

        event.replyEmbeds(embed).setEphemeral(true).queue()
    }

    private fun addActionButtons(thread: ThreadChannel) {
        val closeButton = Button.danger("close-ticket", "❌ Close Ticket")
        val deleteButton = Button.danger("delete-ticket", "🗑️ Delete Ticket")

        thread.sendMessage("Staff, use the buttons below to manage this ticket.")
            .setActionRow(closeButton, deleteButton)
            .queue()
    }

    private fun handleCloseTicket(event: ButtonInteractionEvent) {
        val member = event.member ?: return
        val thread = event.messageChannel as? ThreadChannel ?: return

        val staffRole = member.guild.getRoleById(staffRoleId)
        if (staffRole != null && !member.roles.contains(staffRole)) {
            event.reply("❌ You do not have permission to close this ticket.")
                .setEphemeral(true)
                .queue()
            return
        }

        val userId = openTickets.entries.find { it.value == thread.id }?.key
        if (userId != null) {
            openTickets.remove(userId)
        }

        thread.sendMessage("✅ This ticket has been closed by ${event.user.asMention}.").queue()
        thread.manager.setLocked(true).queue()
        thread.manager.setArchived(true).queue()

        event.reply("✅ Ticket closed successfully.").setEphemeral(true).queue()
    }

    private fun handleDeleteTicket(event: ButtonInteractionEvent) {
        val member = event.member ?: return
        val thread = event.messageChannel as? ThreadChannel

        if (thread == null) {
            event.reply("❌ Unable to find the thread. It might have already been deleted.")
                .setEphemeral(true)
                .queue()
            return
        }

        val staffRole = member.guild.getRoleById(staffRoleId)
        if (staffRole != null && !member.roles.contains(staffRole)) {
            event.reply("❌ You do not have permission to delete this ticket.")
                .setEphemeral(true)
                .queue()
            return
        }

        val userId = openTickets.entries.find { it.value == thread.id }?.key
        if (userId != null) {
            openTickets.remove(userId)
        }

        thread.sendMessage("🗑️ This ticket has been deleted by ${event.user.asMention}.").queue({
            thread.delete().queue()
            event.reply("🗑️ Ticket deleted successfully.").setEphemeral(true).queue()
        }, {
            event.reply("❌ Failed to delete the ticket. It might have already been deleted.")
                .setEphemeral(true)
                .queue()
        })
    }

}

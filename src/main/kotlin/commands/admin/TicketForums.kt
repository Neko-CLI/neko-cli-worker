@file:Suppress("SpellCheckingInspection")

package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
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

class TicketForums : ListenerAdapter() {

    private val api = NekoCLIApi()
    private val forumChannelId = api.getConfig("FORUMCHANNELID")
    private val staffRoleId = api.getConfig("FTTACCESSID")

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "setticketforum") return

        val embed = EmbedBuilder()
            .setTitle("🎫 Open a Ticket")
            .setDescription(
                "If you need assistance, click the **Open Ticket** button below. \n" +
                        "A private thread will be created where you can communicate with the staff."
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
        if (event.componentId == "open-ticket") {
            handleOpenTicket(event)
        }
    }

    private fun handleOpenTicket(event: ButtonInteractionEvent) {
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
        val threadName = "Ticket-${event.user.name}-$timestamp"

        forumChannel.createForumPost(
            threadName,
            MessageCreateBuilder().setContent("Ticket created by ${event.user.asMention}").build()
        ).queue { post ->
            val thread = post.threadChannel
            setupThreadPermissions(thread, event.user.id)

            val ticketUrl = thread.jumpUrl

            val embed = EmbedBuilder()
                .setTitle("🎟️ Ticket Created")
                .setDescription("Your ticket has been created successfully. Staff will assist you shortly.")
                .addField("📎 Ticket Link", "[Click here to access your ticket]($ticketUrl)", false)
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setFooter("Ticket System", event.jda.selfUser.effectiveAvatarUrl)
                .setTimestamp(Instant.now())
                .build()

            event.replyEmbeds(embed).setEphemeral(true).queue()

            val welcomeEmbed = EmbedBuilder()
                .setTitle("Welcome to Your Ticket")
                .setDescription(
                    "Hello ${event.user.asMention}, a member of our staff will assist you shortly. \n" +
                            "Please describe your issue here in detail."
                )
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setFooter("Ticket System", event.jda.selfUser.effectiveAvatarUrl)
                .setTimestamp(Instant.now())
                .build()

            thread.sendMessageEmbeds(welcomeEmbed).queue()
        }
    }

    private fun setupThreadPermissions(thread: ThreadChannel, userId: String) {
        val guild = thread.guild
        val everyoneRole = guild.publicRole
        val staffRole = guild.getRoleById(staffRoleId)

        thread.manager.channel.permissionContainer.apply {
            guild.getMemberById(userId)?.let { member ->
                upsertPermissionOverride(member)
                    .setAllowed(
                        Permission.VIEW_CHANNEL,
                        Permission.MESSAGE_SEND,
                        Permission.MESSAGE_ATTACH_FILES,
                        Permission.MESSAGE_ADD_REACTION,
                        Permission.MESSAGE_HISTORY
                    )
                    .queue()
            }

            upsertPermissionOverride(everyoneRole)
                .setDenied(Permission.VIEW_CHANNEL)
                .queue()

            staffRole?.let {
                upsertPermissionOverride(it)
                    .setAllowed(
                        Permission.VIEW_CHANNEL,
                        Permission.MESSAGE_SEND,
                        Permission.MESSAGE_ATTACH_FILES,
                        Permission.MESSAGE_ADD_REACTION,
                        Permission.MESSAGE_HISTORY
                    )
                    .queue()
            }
        }
    }
}

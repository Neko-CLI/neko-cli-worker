@file:Suppress("DEPRECATION")

package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import utils.NekoCLIApi
import java.awt.Color

class Kick : ListenerAdapter() {
    private val api = NekoCLIApi()
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "kick") return

        val guild = event.guild
        if (guild == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Access Denied**")
                    .setDescription(
                        "This command must be used in a **server**.\n\n" +
                                "📌 **Tip:** Join a server and try again."
                    )
                    .setColor(Color.RED)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val member = event.member
        if (member == null || !member.hasPermission(Permission.KICK_MEMBERS)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Insufficient Permissions**")
                    .setDescription(
                        "You do not have the required permissions to kick members.\n\n" +
                                "🔑 **Required Permission:** `Kick Members`."
                    )
                    .setColor(Color.RED)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val target: Member? = event.getOption("user")?.asMember
        val reason = event.getOption("reason")?.asString ?: "No reason provided"

        if (target == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Invalid Target**")
                    .setDescription(
                        "You must specify a valid member to kick.\n\n" +
                                "📌 **Tip:** Mention the user or provide their ID."
                    )
                    .setColor(Color.RED)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (!member.canInteract(target)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Cannot Kick Target**")
                    .setDescription(
                        "You cannot kick this user due to **role hierarchy restrictions** or insufficient permissions.\n\n" +
                                "🔒 **Tip:** Ensure your role is higher than the target's role."
                    )
                    .setColor(Color.RED)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        // Notify the user before kicking
        target.user.openPrivateChannel().queue({ channel ->
            channel.sendMessageEmbeds(
                EmbedBuilder()
                    .setTitle("🔔 **You Have Been Kicked**")
                    .setDescription(
                        "You have been kicked from **${guild.name}**.\n\n" +
                                "✍️ **Reason:** $reason\n" +
                                "👮 **Kicked By:** ${event.user.asMention}"
                    )
                    .setColor(Color.RED)
                    .setFooter("You can rejoin the server using the link below.")
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setActionRow(
                Button.link(api.getConfig("DISCORDSERVER"), "Rejoin Server")
            ).queue({
                guild.kick(target, reason).queue({
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("✅ **Success**")
                            .setDescription("Successfully kicked **${target.user.name}**.")
                            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                            .addField("👤 **Kicked User**", target.user.asMention, true)
                            .addField("✍️ **Reason**", reason, true)
                            .addField("👮 **Kicked By**", event.user.asMention, true)
                            .setTimestamp(event.timeCreated)
                            .build()
                    ).queue()
                }, { error ->
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("❌ **Error**")
                            .setDescription(
                                "An error occurred while trying to kick **${target.user.name}**:\n\n" +
                                        "📌 **Details:** ${error.message}"
                            )
                            .setColor(Color.RED)
                            .setTimestamp(event.timeCreated)
                            .build()
                    ).setEphemeral(true).queue()
                })
            }, { error ->
                event.replyEmbeds(
                    EmbedBuilder()
                        .setTitle("❌ **Error**")
                        .setDescription(
                            "Failed to notify **${target.user.name}** about the kick.\n\n" +
                                    "📌 **Details:** ${error.message}"
                        )
                        .setColor(Color.RED)
                        .setTimestamp(event.timeCreated)
                        .build()
                ).setEphemeral(true).queue()
            })
        }, { error ->
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Error**")
                    .setDescription(
                        "Could not open a private channel with **${target.user.name}**.\n\n" +
                                "📌 **Details:** ${error.message}"
                    )
                    .setColor(Color.RED)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
        })
    }
}

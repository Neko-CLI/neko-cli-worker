@file:Suppress("DEPRECATION")

package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
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
                    .setTitle("❌ Access Denied")
                    .setDescription("This command must be used in a server.")
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
                    .setTitle("❌ Insufficient Permissions")
                    .setDescription("You do not have permission to kick members.")
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
                    .setTitle("❌ Invalid Target")
                    .setDescription("You must specify a valid member to kick.")
                    .setColor(Color.RED)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (!member.canInteract(target)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Cannot Kick Target")
                    .setDescription("You cannot kick this user due to role hierarchy or insufficient permissions.")
                    .setColor(Color.RED)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        guild.kick(target, reason).queue({
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ Success")
                    .setDescription("Successfully kicked ${target.user.name}.")
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .addField("Reason", reason, false)
                    .addField("Kicked By", event.user.name, true)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).queue()
        }, { error ->
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Error")
                    .setDescription("An error occurred while kicking ${target.user.name}: ${error.message}")
                    .setColor(Color.RED)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
        })
    }
}

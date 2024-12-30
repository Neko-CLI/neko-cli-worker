package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import utils.NekoCLIApi
import java.awt.Color
import java.time.Duration
import java.time.Instant

class TimeOut : ListenerAdapter() {
    private val api = NekoCLIApi()
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "timeout") return

        val guild = event.guild
        if (guild == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Access Denied")
                    .setDescription("This command must be used in a server.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val member = event.member
        if (member == null || !member.hasPermission(Permission.MODERATE_MEMBERS)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Insufficient Permissions")
                    .setDescription("You do not have permission to timeout members.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val target = event.getOption("user")?.asMember
        val durationOption = event.getOption("duration")?.asString
        val reason = event.getOption("reason")?.asString ?: "No reason provided"

        if (target == null || durationOption == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Invalid Arguments")
                    .setDescription("You must specify a valid user and duration.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (!member.canInteract(target)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Cannot Timeout Target")
                    .setDescription("You cannot timeout this user due to role hierarchy or insufficient permissions.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val timeoutDuration = parseDuration(durationOption)
        if (timeoutDuration == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Invalid Duration")
                    .setDescription("The specified duration is not valid. Use formats like `1h`, `30m`, `1d`.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val timeoutUntil = Instant.now().plus(timeoutDuration)
        target.timeoutFor(timeoutDuration).reason(reason).queue({
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ Success")
                    .setDescription("Successfully timed out ${target.user.name} for $durationOption.")
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .addField("Reason", reason, false)
                    .addField("Timeout Until", timeoutUntil.toString(), true)
                    .build()
            ).queue()
        }, {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Error")
                    .setDescription("Failed to timeout the user: ${it.message}")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
        })
    }

    private fun parseDuration(duration: String): Duration? {
        val regex = Regex("(\\d+)([smhd])")
        val match = regex.matchEntire(duration) ?: return null

        val amount = match.groupValues[1].toLong()
        val unit = match.groupValues[2]

        return when (unit) {
            "s" -> Duration.ofSeconds(amount)
            "m" -> Duration.ofMinutes(amount)
            "h" -> Duration.ofHours(amount)
            "d" -> Duration.ofDays(amount)
            else -> null
        }
    }
}

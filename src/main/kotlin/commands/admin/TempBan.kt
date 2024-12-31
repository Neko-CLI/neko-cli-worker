@file:Suppress("SpellCheckingInspection", "CanBeParameter")

package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.bson.Document
import utils.MongoDBManager
import utils.NekoCLIApi
import java.awt.Color
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.fixedRateTimer

class TempBan(
    private val mongoManager: MongoDBManager,
    private val api: NekoCLIApi
) : ListenerAdapter() {
    private val jda = api.getJdaInstance()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "tempban") return

        when (event.subcommandName) {
            "add" -> handleAddTempBan(event)
            "list" -> handleListBans(event)
            "remove" -> handleRemoveBan(event)
            else -> event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Unknown Subcommand**")
                    .setDescription("The specified subcommand is not recognized.")
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
        }
    }

    private fun handleAddTempBan(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Error**")
                    .setDescription("This command must be used in a server.")
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val member = event.member ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Permission Denied**")
                    .setDescription("You do not have permission to use this command.")
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (!member.hasPermission(Permission.BAN_MEMBERS)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("🔒 **Permission Denied**")
                    .setDescription("You do not have permission to ban members.")
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val target = event.getOption("user")?.asUser ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Invalid Target**")
                    .setDescription("You must specify a valid user to ban.")
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val durationOption = event.getOption("duration")?.asString ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Missing Arguments**")
                    .setDescription("You must specify a valid duration for the ban.")
                    .setColor(Color.YELLOW)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val reason = event.getOption("reason")?.asString ?: "No reason provided"

        val duration = parseDuration(durationOption)
        if (duration == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Invalid Duration**")
                    .setDescription("The duration format is invalid. Use formats like `1h`, `30m`, or `1d`.")
                    .setColor(Color.YELLOW)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val unbanTime = Date.from(Instant.now().plus(duration))
        val inviteLink = api.getConfig("DISCORDSERVER")

        target.openPrivateChannel().queue({ privateChannel ->
            privateChannel.sendMessageEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Temporary Ban Notice**")
                    .setDescription(
                        "You are temporarily banned from **${guild.name}**.\n\n" +
                                "**Reason:** $reason\n" +
                                "**Duration:** $durationOption\n" +
                                "**Unban Time:** ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(unbanTime.toInstant().atZone(ZoneId.systemDefault()))}\n" +
                                "**Banned By:** ${event.user.name}"
                    )
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setTimestamp(Instant.now())
                    .build()
            ).setActionRow(
                Button.link(inviteLink, "Rejoin Server")
            ).queue()

            guild.ban(listOf(UserSnowflake.fromId(target.id)), Duration.ZERO).reason(reason).queue({
                val banRecord = Document()
                    .append("userId", target.id)
                    .append("guildId", guild.id)
                    .append("unbanTime", unbanTime)
                mongoManager.insertBans(banRecord)

                event.replyEmbeds(
                    EmbedBuilder()
                        .setTitle("✅ **User Banned Temporarily**")
                        .setDescription(
                            "**User:** ${target.name}\n" +
                                    "**Duration:** $durationOption\n" +
                                    "**Reason:** $reason\n" +
                                    "**Unban Time:** ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(unbanTime.toInstant().atZone(ZoneId.systemDefault()))}"
                        )
                        .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                        .setTimestamp(Instant.now())
                        .build()
                ).queue()
            }, {
                event.reply("❌ Failed to ban the user. Ensure I have the required permissions.").setEphemeral(true).queue()
            })
        }, {
            event.reply("⚠️ Could not send a DM to ${target.asMention}. Proceeding with the ban.").setEphemeral(true).queue()
        })
    }

    private fun handleListBans(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Error**")
                    .setDescription("This command must be used in a server.")
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val bans = mongoManager.findBans(Document("guildId", guildId))

        if (bans.isEmpty()) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ **No Active Temporary Bans**")
                    .setDescription("There are no active temporary bans.")
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val banList = bans.joinToString(separator = "\n") {
            val userId = it.getString("userId")
            val unbanTime = (it["unbanTime"] as Date).toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            "User ID: `$userId`, Unban Time: `$unbanTime`"
        }

        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("📜 **Active Temporary Bans**")
                .setDescription("**List of Active Bans:**\n\n$banList")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setTimestamp(Instant.now())
                .build()
        ).setEphemeral(true).queue()
    }

    private fun handleRemoveBan(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Error**")
                    .setDescription("This command must be used in a server.")
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val userId = event.getOption("user")?.asString ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Missing Arguments**")
                    .setDescription("You must specify a valid user ID.")
                    .setColor(Color.YELLOW)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val banRecord = mongoManager.findBans(
            Document("userId", userId).append("guildId", guild.id)
        ).firstOrNull()

        if (banRecord == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Ban Not Found**")
                    .setDescription("No active ban found for the specified user.")
                    .setColor(Color.YELLOW)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        guild.unban(UserSnowflake.fromId(userId.toLong())).queue({
            mongoManager.deleteBans(Document("_id", banRecord["_id"]))

            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ **Ban Removed**")
                    .setDescription(
                        "The ban for **User ID:** `$userId` has been successfully removed.\n\n" +
                                "🎉 The user is now free to rejoin the server if permitted."
                    )
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setTimestamp(Instant.now())
                    .build()
            ).queue()
        }, {
            event.reply("❌ Failed to remove the ban.").setEphemeral(true).queue()
        })
    }

    init {
        fixedRateTimer("UnbanTimer", true, 0L, 1000L) {
            val now = Date()
            val expiredBans = mongoManager.findBans(Document("unbanTime", Document("\$lte", now)))

            for (ban in expiredBans) {
                val guildId = ban.getString("guildId")
                val userId = ban.getString("userId")

                mongoManager.deleteBans(Document("_id", ban["_id"]))

                val guild = jda.getGuildById(guildId)
                if (guild != null) {
                    guild.unban(UserSnowflake.fromId(userId.toLong())).queue({
                        println("[Info] Unbanned user $userId from guild $guildId.")
                    }, {
                        println("[Error] Failed to unban user $userId from guild $guildId: ${it.message}")
                    })
                } else {
                    println("[Warning] Guild $guildId not found for unbanning user $userId.")
                }
            }
        }
    }

    private fun parseDuration(duration: String): Duration? {
        val regex = Regex("(\\d+)([smhd])")
        val match = regex.matchEntire(duration) ?: return null

        val value = match.groupValues[1].toLong()
        return when (match.groupValues[2]) {
            "s" -> Duration.ofSeconds(value)
            "m" -> Duration.ofMinutes(value)
            "h" -> Duration.ofHours(value)
            "d" -> Duration.ofDays(value)
            else -> null
        }
    }
}

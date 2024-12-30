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
            else -> event.reply("❌ Unknown subcommand!").setEphemeral(true).queue()
        }
    }

    private fun handleAddTempBan(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.reply("❌ This command must be used in a server.").setEphemeral(true).queue()
            return
        }

        val member = event.member ?: run {
            event.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue()
            return
        }

        if (!member.hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("❌ You do not have permission to ban members.").setEphemeral(true).queue()
            return
        }

        val target = event.getOption("user")?.asUser ?: run {
            event.reply("❌ You must specify a valid user.").setEphemeral(true).queue()
            return
        }

        val durationOption = event.getOption("duration")?.asString ?: run {
            event.reply("❌ You must specify a valid duration.").setEphemeral(true).queue()
            return
        }

        val reason = event.getOption("reason")?.asString ?: "No reason provided"

        val duration = parseDuration(durationOption)
        if (duration == null) {
            event.reply("❌ Invalid duration format. Use formats like `1h`, `30m`, or `1d`.").setEphemeral(true).queue()
            return
        }

        val unbanTime = Date.from(Instant.now().plus(duration))
        val inviteLink = api.getConfig("DISCORDSERVER")

        target.openPrivateChannel().queue({ privateChannel ->
            privateChannel.sendMessageEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Temporary Ban Notice**")
                    .setDescription(
                        """
        You are about to be temporarily banned from **${guild.name}**.
        
        **Details:**
        - **Reason:** $reason
        - **Duration:** $durationOption
        - **Unban Time:** `${unbanTime}`
        - **Banned By:** *${event.user.name}*
        """.trimIndent()
                    )
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setTimestamp(Instant.now())
                    .build()
            ).setActionRow(
                Button.link(inviteLink, "Rejoin Server")
            ).queue({
                guild.ban(listOf(UserSnowflake.fromId(target.id)), Duration.ofSeconds(0)).reason(reason).queue({
                    val banRecord = Document()
                        .append("userId", target.id)
                        .append("guildId", guild.id)
                        .append("unbanTime", unbanTime)
                    mongoManager.insertBans(banRecord)

                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("✅ **User Banned Temporarily**")
                            .setDescription(
                                """
        **Details:**
        - **User Banned:** ${target.name}
        - **Duration:** $durationOption
        - **Reason:** $reason
        - **Unban Time:** `${unbanTime}`
        """.trimIndent()
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
        }, {
            event.reply("⚠️ Could not open a private channel with ${target.asMention}. Proceeding with the ban.").setEphemeral(true).queue()
        })
    }


    private fun handleListBans(event: SlashCommandInteractionEvent) {
        val guildId = event.guild?.id ?: run {
            event.reply("❌ This command must be used in a server.").setEphemeral(true).queue()
            return
        }

        val bans = mongoManager.findBans(Document("guildId", guildId))

        if (bans.isEmpty()) {
            event.reply("✅ No active temporary bans found.").setEphemeral(true).queue()
            return
        }

        val banList = bans.joinToString(separator = "\n") {
            val userId = it.getString("userId")
            val unbanTime = (it["unbanTime"] as Date).toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            "User ID: `$userId`, Unban Time: $unbanTime"
        }

        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("📜 **Active Temporary Bans**")
                .setDescription(
                    """
        **List of Active Bans:**
        
        $banList
        """.trimIndent()
                )
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setTimestamp(Instant.now())
                .build()

        ).setEphemeral(true).queue()
    }

    private fun handleRemoveBan(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.reply("❌ This command must be used in a server.").setEphemeral(true).queue()
            return
        }

        val userId = event.getOption("user")?.asString ?: run {
            event.reply("❌ You must specify a valid user ID.").setEphemeral(true).queue()
            return
        }

        val banRecord = mongoManager.findBans(
            Document("userId", userId).append("guildId", guild.id)
        ).firstOrNull()

        if (banRecord == null) {
            event.reply("❌ No active ban found for the specified user.").setEphemeral(true).queue()
            return
        }

        guild.unban(UserSnowflake.fromId(userId.toLong())).queue({
            mongoManager.deleteBans(Document("_id", banRecord["_id"]))

            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ **Ban Removed**")
                    .setDescription(
                        """
        The ban for **User ID:** `$userId` has been successfully removed.
        
        🎉 The user is now free to rejoin the server if permitted.
        """.trimIndent()
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

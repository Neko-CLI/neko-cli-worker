@file:Suppress("SENSELESS_COMPARISON")

package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bson.Document
import utils.MongoDBManager
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant
import java.util.*

class Warn(
    private val mongoManager: MongoDBManager,
) : ListenerAdapter() {
    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "warn") return

        when (event.subcommandName) {
            "add" -> handleAddWarn(event)
            "info" -> handleWarnInfo(event)
            "remove" -> handleRemoveWarn(event)
            else -> event.reply("❌ Unknown subcommand!").setEphemeral(true).queue()
        }
    }

    private fun handleAddWarn(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Command Error")
                    .setDescription("This command must be used in a **server** context.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val member = event.member ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Permission Denied")
                    .setDescription("You do not have permission to use this command.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (!member.hasPermission(Permission.MANAGE_ROLES, Permission.ADMINISTRATOR)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Permission Denied")
                    .setDescription("You lack the required permissions to use this command.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val target = event.getOption("user")?.asMember ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Invalid Target")
                    .setDescription("You must specify a valid **user** to warn.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val reason = event.getOption("reason")?.asString ?: "No reason provided"
        val warnRoleIds = listOf(
            api.getConfig("WARN1ROLEID"),
            api.getConfig("WARN2ROLEID"),
            api.getConfig("WARN3ROLEID")
        )

        if (warnRoleIds.any { it == null || it.isBlank() }) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Configuration Error")
                    .setDescription("Warn roles are not configured properly. Please contact an administrator.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val currentWarns = mongoManager.findWarnings(
            Document("userId", target.id).append("guildId", guild.id)
        )

        val nextWarnLevel = currentWarns.size

        if (nextWarnLevel >= 2) {
            val banEmbed = EmbedBuilder()
                .setTitle("🚫 **Banned Due to Warnings**")
                .setDescription(
                    """
            The user ${target.asMention} has reached **3 warnings** and has been **permanently banned**.

            **Reason for Last Warning:** `$reason`
            """.trimIndent()
                )
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setTimestamp(Instant.now())
                .build()

            target.user.openPrivateChannel().queue({ channel ->
                channel.sendMessageEmbeds(banEmbed).queue({
                    guild.ban(listOf(UserSnowflake.fromId(target.id)), null).reason("Reached [3/3] warnings").queue {
                        mongoManager.deleteWarnings(Document("userId", target.id).append("guildId", guild.id))
                        event.replyEmbeds(banEmbed).queue()
                    }
                }, {
                    event.hook.sendMessage("⚠️ Could not notify ${target.asMention} via direct message.").queue()
                    guild.ban(listOf(UserSnowflake.fromId(target.id)), null).reason("Reached 3 warnings").queue {
                        mongoManager.deleteWarnings(Document("userId", target.id).append("guildId", guild.id))
                        event.replyEmbeds(banEmbed).queue()
                    }
                })
            }, {
                event.hook.sendMessage("⚠️ Could not open a DM channel with ${target.asMention}.").queue()
                guild.ban(listOf(UserSnowflake.fromId(target.id)), null).reason("Reached 3 warnings").queue {
                    mongoManager.deleteWarnings(Document("userId", target.id).append("guildId", guild.id))
                    event.replyEmbeds(banEmbed).queue()
                }
            })
            return
        }

        val warnRoleId = warnRoleIds[nextWarnLevel]
        val warnRole = guild.getRoleById(warnRoleId) ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Missing Role")
                    .setDescription("Could not find warn role for level ${nextWarnLevel + 1}.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        guild.addRoleToMember(target, warnRole).queue {
            val expireAt = Date.from(Instant.now().plusSeconds(7 * 24 * 60 * 60))
            val warnRecord = Document()
                .append("userId", target.id)
                .append("guildId", guild.id)
                .append("reason", reason)
                .append("warnLevel", nextWarnLevel + 1)
                .append("expiresAt", expireAt)
            mongoManager.insertWarning(warnRecord)

            val userEmbed = EmbedBuilder()
                .setTitle("⚠️ **You Have Been Warned**")
                .setDescription(
                    """
        You have received a warning in the server **${guild.name}**.

        **Details:**
        - **Reason:** `$reason`
        - **Warn Level:** `${nextWarnLevel + 1}/3`
        - **Expires At:** `${expireAt}`

        ⚠️ **Important:** 
        If you reach **3 warnings**, you will be **permanently banned** from the server.
        """.trimIndent()
                )
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setTimestamp(Instant.now())
                .build()

            target.user.openPrivateChannel().queue { channel ->
                channel.sendMessageEmbeds(userEmbed).queue({}, {
                    event.hook.sendMessage("⚠️ Could not notify ${target.asMention} via direct message.").queue()
                })
            }
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **User Warned**")
                    .setDescription(
                        """
        The user ${target.asMention} has been **warned**.

        **Details:**
        - **Reason:** `$reason`
        - **Warn Level:** `${nextWarnLevel + 1}/3`
        - **Expires At:** `${expireAt}`
        """.trimIndent()
                    )
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setTimestamp(Instant.now())
                    .build()

            ).queue()
        }
    }

    private fun handleWarnInfo(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Command Error")
                    .setDescription("This command must be used in a **server** context.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val target = event.getOption("user")?.asMember ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Invalid Target")
                    .setDescription("You must specify a valid **user** to view warnings.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val currentWarns = mongoManager.findWarnings(
            Document("userId", target.id).append("guildId", guild.id)
        )

        if (currentWarns.isEmpty()) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ No Active Warnings")
                    .setDescription("${target.asMention} has no active warnings.")
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val warnInfo = currentWarns.joinToString(separator = "\n") {
            val expiresAt = it.getDate("expiresAt")
            val warnLevel = it.getInteger("warnLevel")
            "- **Level:** `$warnLevel` | **Expires:** `$expiresAt`"
        }

        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("⚠️ **Active Warnings for ${target.user.name}**")
                .setDescription(
                    """
        **Warnings Summary:**
        $warnInfo
        """.trimIndent()
                )
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setTimestamp(Instant.now())
                .build()
        ).queue()
    }

    private fun handleRemoveWarn(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Command Error")
                    .setDescription("This command must be used in a **server** context.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val target = event.getOption("user")?.asMember ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Invalid Target")
                    .setDescription("You must specify a valid **user** to remove warnings.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val currentWarns = mongoManager.findWarnings(
            Document("userId", target.id).append("guildId", guild.id)
        )

        if (currentWarns.isEmpty()) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ No Active Warnings")
                    .setDescription("${target.asMention} has no active warnings.")
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val latestWarn = currentWarns.maxByOrNull { it.getDate("expiresAt") } ?: return
        val warnLevel = latestWarn.getInteger("warnLevel")

        val warnRoleIds = listOf(
            api.getConfig("WARN1ROLEID"),
            api.getConfig("WARN2ROLEID"),
            api.getConfig("WARN3ROLEID")
        )

        val warnRoleId = warnRoleIds[warnLevel - 1]
        val warnRole = guild.getRoleById(warnRoleId)

        guild.removeRoleFromMember(target, warnRole!!).queue {
            mongoManager.deleteWarnings(Document("_id", latestWarn["_id"]))

            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ **Warning Removed**")
                    .setDescription(
                        """
                        **Details:**
                        - **Warning Level Removed:** $warnLevel
                        - **User:** ${target.asMention}
                        """.trimIndent()
                    )
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setTimestamp(Instant.now())
                    .build()
            ).queue()
        }
    }
}

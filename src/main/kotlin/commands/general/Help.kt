@file:Suppress("SpellCheckingInspection")

package commands.general

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import utils.NekoCLIApi
import java.awt.Color

class Help : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        try {
            when (event.name) {
                "help" -> {
                    if (event.guild?.id.equals(api.getConfig("GUILDID"))) {
                        val embed = EmbedBuilder()
                            .setTitle("📖 **Neko-CLI-Worker Help Menu** 🐈‍⬛")
                            .setDescription(
                                "Use the menu below to navigate through the available command categories. " +
                                        "Each section provides detailed descriptions and usage examples."
                            )
                            .setImage(api.getConfig("SERVERIMAGE"))
                            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                            .setAuthor(
                                event.jda.selfUser.name,
                                api.getConfig("WEBSITE"),
                                event.jda.selfUser.avatarUrl
                            )
                            .setFooter("Help requested by ${event.user.name}", event.user.effectiveAvatarUrl)
                            .setTimestamp(event.timeCreated)
                            .build()

                        val menu = StringSelectMenu.create("help-menu")
                            .setPlaceholder("🔍 Select a command category...")
                            .addOption("🌀 General Commands", "general", "Explore everyday commands.")
                            .addOption("🔒 Admin Commands", "admin", "Manage server with admin commands.")
                            .addOption("⚙️ Utility Commands", "utility", "Access helpful tools and utilities.")
                            .build()

                        event.replyEmbeds(embed).addActionRow(menu).setEphemeral(true).queue()
                    } else {
                        event.replyEmbeds(
                            EmbedBuilder()
                                .setTitle("❌ **Access Denied**")
                                .setDescription(
                                    "You are not in the **NekoCLI server**. Access to this command is restricted."
                                )
                                .setImage(api.getConfig("SERVERIMAGE"))
                                .setColor(Color.RED)
                                .setAuthor(
                                    event.jda.selfUser.name,
                                    api.getConfig("WEBSITE"),
                                    event.jda.selfUser.avatarUrl
                                )
                                .setTimestamp(event.timeCreated)
                                .build()
                        ).setEphemeral(true).queue()
                    }
                }
            }
        } catch (e: Exception) {
            AnsiConsole.systemInstall()
            println(ansi().fgBrightRed().a("[Error]").reset().a(" An error occurred: ${e.message}"))
            e.printStackTrace()
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        try {
            when (event.selectMenu.id) {
                "help-menu" -> {
                    val embed = when (event.values.first()) {
                        "general" -> EmbedBuilder()
                            .setTitle("🌀 **General Commands**")
                            .setDescription(
                                """```yml
/help - 📖 Display this help menu.
/status - 🌐 Check Neko-CLI, website, and bot status.
/announce - 📢 Create a server-wide announcement.
/suggest - 💡 Submit a suggestion or idea.
/bugreport - 🐞 Report a bug or issue.
/passgen - 🔐 Generate a secure password with style.
```"""
                            )
                            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                            .setFooter("Category: General Commands", event.user.effectiveAvatarUrl)
                            .build()

                        "admin" -> {
                            if (event.member?.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) == true) {
                                EmbedBuilder()
                                    .setTitle("🔒 **Admin Commands**")
                                    .setDescription(
                                        """```yml
/clear - 🗑️ Clear messages in a channel.
/ban - 🚫 Ban a user from the server.
/pex - 🛠️ Grant a role to a user.
/depex - ❌ Revoke a role from a user.
/stopbot - ⛔ Shut down the bot (admin only).
/sponsors - 🎉 Manage and showcase sponsors.
/setasktohrmodal - 💬 Configure the Ask HR Modal.
/setverificationchannel - 🔑 Set up the verification system.
/setticketforum - 🎟️ Set up a ticket system for user support.
/userinfo - 👤 Get detailed user information.
/tempban - ⏳ Manage temporary bans.
/kick - 🥾 Kick a user from the server.
/timeout - ⏱️ Temporarily timeout a user.
/warn - ⚠️ Manage user warnings.
```"""
                                    )
                                    .setColor(Color.RED)
                                    .setFooter("Category: Admin Commands", event.user.effectiveAvatarUrl)
                                    .build()
                            } else {
                                EmbedBuilder()
                                    .setTitle("❌ **Access Denied**")
                                    .setDescription(
                                        "You do not have permission to view admin commands. Please contact a server administrator."
                                    )
                                    .setColor(Color.RED)
                                    .setFooter("Permission Required", event.user.effectiveAvatarUrl)
                                    .build()
                            }
                        }

                        "utility" -> EmbedBuilder()
                            .setTitle("⚙️ **Utility Commands**")
                            .setDescription(
                                """```yml
/dependencies - 🔍 Search for a package name.
/snapcode - ✨ Generate a stylish code snapshot.
```"""
                            )
                            .setColor(Color.YELLOW)
                            .setFooter("Category: Utility Commands", event.user.effectiveAvatarUrl)
                            .build()

                        else -> null
                    }

                    if (embed != null) {
                        event.replyEmbeds(embed).setEphemeral(true).queue()
                    }
                }
            }
        } catch (e: Exception) {
            AnsiConsole.systemInstall()
            println(ansi().fgBrightRed().a("[Error]").reset().a(" An error occurred: ${e.message}"))
            e.printStackTrace()
        }
    }
}

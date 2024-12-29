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
                            .setTitle("Neko-CLI-Worker Help Menu 🐈‍⬛")
                            .setDescription("Navigate through the available commands using the menu below.")
                            .setImage(api.getConfig("SERVERIMAGE"))
                            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                            .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                            .setTimestamp(event.timeCreated)
                            .build()

                        val menu = StringSelectMenu.create("help-menu")
                            .setPlaceholder("Select a command category...")
                            .addOption("General Commands", "general", "Basic commands for everyday use.")
                            .addOption("Admin Commands", "admin", "Commands for server administrators.")
                            .addOption("Utility Commands", "utility", "Helpful tools and utilities.")
                            .build()

                        event.replyEmbeds(embed).addActionRow(menu).setEphemeral(true).queue()
                    } else {
                        event.replyEmbeds(
                            EmbedBuilder()
                                .setTitle("Error ❌")
                                .setDescription("You are not in the NekoCLI server, so you cannot use this command.")
                                .setImage(api.getConfig("SERVERIMAGE"))
                                .setColor(Color.RED)
                                .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
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
                            .setTitle("General Commands 🍴")
                            .setDescription(
                                """```yml
/help - Displays this help menu.
/status - Check the status of Neko-CLI, its website, and the bot!
/announce - Create a server-wide announcement.
/suggest - Submit your suggestion or idea!
/bugreport - Report a bug or issue you encountered!
/passgen - Generate your custom secure password with style!
```"""
                            )
                            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                            .build()

                        "admin" -> EmbedBuilder()
                            .setTitle("Admin Commands 🛠️")
                            .setDescription(
                                """```yml
/clear - Clear messages in the channel
/ban - Ban a user from the server.
/pex - Grant a role to a user.
/depex - Revoke a role from a user.
/stopbot - Shut down the bot (admin only).
/sponsors - Manage and showcase your sponsors.
/setasktohrmodal - Configure and Enable the Ask to HR Modal for Questions
/setverificationchannel - Set up the advanced verification system in the current channel
/setticketforum - Set up a ticket system for users to create private threads for support.
```"""
                            )
                            .setColor(Color.RED)
                            .build()

                        "utility" -> EmbedBuilder()
                            .setTitle("Utility Commands 🔧")
                            .setDescription(
                                """```yml
/userinfo - Fetch detailed information about a user.
/dependencies - Name of the package to search for.
/snapcode - Generate a stylish code snapshot.
```"""
                            )
                            .setColor(Color.YELLOW)
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

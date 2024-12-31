@file:Suppress("SameParameterValue", "SpellCheckingInspection", "NullableBooleanElvis")

package commands.admin

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import utils.NekoCLIApi
import java.awt.Color
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@Serializable
data class Sponsor(
    val name: String,
    val logo: String,
    val link: String,
    val color: String,
    val iconOnly: Boolean,
    val iconSize: Int?
)

@Serializable
data class GitHubContentResponse(
    val content: String,
    val sha: String
)

class Sponsors : ListenerAdapter() {

    private val api = NekoCLIApi()
    private val token = api.getEnv("GITTOKEN")
    private val rawUrl = "https://raw.githubusercontent.com/Neko-CLI/SponsorsJson/master/sponsors.json"
    private val repoApiUrl = "https://api.github.com/repos/Neko-CLI/SponsorsJson/contents/sponsors.json"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private var cachedSponsors: List<Sponsor>? = null

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "sponsor") return

        when (event.subcommandName) {
            "list" -> handleListSponsors(event)
            "add" -> {
                if (event.member?.hasPermission(Permission.ADMINISTRATOR) == true) {
                    handleAddSponsor(event)
                } else {
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("🔒 **Permission Denied**")
                            .setDescription("❌ You do not have permission to add sponsors.")
                            .setColor(Color.RED)
                            .setFooter("Permission validation failed", event.jda.selfUser.avatarUrl)
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
            "remove" -> {
                if (event.member?.hasPermission(Permission.ADMINISTRATOR) == true) {
                    handleRemoveSponsor(event)
                } else {
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("🔒 **Permission Denied**")
                            .setDescription("❌ You do not have permission to remove sponsors.")
                            .setColor(Color.RED)
                            .setFooter("Permission validation failed", event.jda.selfUser.avatarUrl)
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
            else -> event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Unknown Subcommand**")
                    .setDescription("The specified subcommand is not recognized.")
                    .setColor(Color.RED)
                    .build()
            ).setEphemeral(true).queue()
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (event.componentId != "sponsor-select") return

        val selectedSponsorName = event.selectedOptions.firstOrNull()?.value ?: return
        val sponsors = fetchSponsors() ?: return
        val sponsor = sponsors.find { it.name == selectedSponsorName } ?: return

        val embed = EmbedBuilder()
            .setTitle(sponsor.name)
            .setImage(sponsor.logo)
            .setDescription("[Visit ${sponsor.name} Website](${sponsor.link})")
            .setColor(Color.decode(if (sponsor.color.startsWith("#")) sponsor.color else "#${sponsor.color}"))
            .setFooter("Sponsor Details", event.jda.selfUser.avatarUrl)
            .build()

        event.replyEmbeds(embed).setEphemeral(true).queue()
    }

    private fun handleListSponsors(event: SlashCommandInteractionEvent) {
        val sponsors = fetchSponsors() ?: run {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Failed to Fetch Sponsors**")
                    .setDescription("An error occurred while retrieving the list of sponsors.")
                    .setColor(Color.RED)
                    .setFooter("Error fetching sponsors", event.jda.selfUser.avatarUrl)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (sponsors.isEmpty()) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("📋 **No Sponsors Found**")
                    .setDescription("It seems there are no sponsors currently available.")
                    .setColor(Color.YELLOW)
                    .setFooter("Sponsor list empty", event.jda.selfUser.avatarUrl)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val selectMenu = StringSelectMenu.create("sponsor-select")
            .setPlaceholder("Select a sponsor to view details ✨")
            .apply {
                sponsors.forEach { sponsor ->
                    addOption(sponsor.name, sponsor.name, "Click to view details")
                }
            }
            .build()

        event.reply("📋 **Select a sponsor from the menu below:**")
            .setComponents(ActionRow.of(selectMenu))
            .setEphemeral(true)
            .queue()
    }

    private fun handleAddSponsor(event: SlashCommandInteractionEvent) {
        val name = event.getOption("name")?.asString
        val logo = event.getOption("imagelink")?.asString
        val link = event.getOption("link")?.asString
        val color = event.getOption("color")?.asString ?: "ffffff"
        val iconOnly = event.getOption("icononly")?.asBoolean ?: false
        val iconSize = event.getOption("iconsize")?.asInt

        if (name == null || logo == null || link == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Missing Arguments**")
                    .setDescription("❌ Missing required arguments for adding a sponsor.")
                    .setColor(Color.YELLOW)
                    .setFooter("Add sponsor error", event.jda.selfUser.avatarUrl)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val sponsors = fetchSponsors()?.toMutableList() ?: mutableListOf()

        if (sponsors.any { it.name.equals(name, ignoreCase = true) }) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Duplicate Sponsor**")
                    .setDescription("❌ A sponsor with this name already exists.")
                    .setColor(Color.YELLOW)
                    .setFooter("Duplicate sponsor detected", event.jda.selfUser.avatarUrl)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        sponsors.add(Sponsor(name, logo, link, color, iconOnly, iconSize))

        if (updateSponsors(sponsors)) {
            cachedSponsors = sponsors
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ **Sponsor Added Successfully**")
                    .setDescription("The sponsor **$name** has been added successfully!")
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setFooter("Add sponsor success", event.jda.selfUser.avatarUrl)
                    .build()
            ).queue()
        } else {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Failed to Add Sponsor**")
                    .setDescription("An error occurred while adding the sponsor.")
                    .setColor(Color.RED)
                    .setFooter("Add sponsor failure", event.jda.selfUser.avatarUrl)
                    .build()
            ).setEphemeral(true).queue()
        }
    }

    private fun handleRemoveSponsor(event: SlashCommandInteractionEvent) {
        val name = event.getOption("name")?.asString

        if (name == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Missing Arguments**")
                    .setDescription("❌ Missing sponsor name for removal.")
                    .setColor(Color.YELLOW)
                    .setFooter("Remove sponsor error", event.jda.selfUser.avatarUrl)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val sponsors = fetchSponsors()?.toMutableList() ?: mutableListOf()

        if (!sponsors.removeIf { it.name.equals(name, ignoreCase = true) }) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Sponsor Not Found**")
                    .setDescription("❌ The sponsor **$name** was not found.")
                    .setColor(Color.YELLOW)
                    .setFooter("Remove sponsor failure", event.jda.selfUser.avatarUrl)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (updateSponsors(sponsors)) {
            cachedSponsors = sponsors
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ **Sponsor Removed Successfully**")
                    .setDescription("The sponsor **$name** has been removed successfully.")
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setFooter("Remove sponsor success", event.jda.selfUser.avatarUrl)
                    .build()
            ).queue()
        } else {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Failed to Remove Sponsor**")
                    .setDescription("An error occurred while removing the sponsor.")
                    .setColor(Color.RED)
                    .setFooter("Remove sponsor failure", event.jda.selfUser.avatarUrl)
                    .build()
            ).setEphemeral(true).queue()
        }
    }

    private fun fetchSponsors(): List<Sponsor>? {
        if (cachedSponsors != null) return cachedSponsors

        return try {
            val connection = URL(rawUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != 200) {
                println("Failed to fetch sponsors: ${connection.responseMessage}")
                return null
            }

            val response = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<List<Sponsor>>(response).also {
                cachedSponsors = it
            }
        } catch (e: Exception) {
            println("Error fetching sponsors: ${e.message}")
            null
        }
    }

    private fun updateSponsors(sponsors: List<Sponsor>): Boolean {
        try {
            val currentData = fetchGitHubContent() ?: return false

            val connection = URL(repoApiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "token $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val payload = json.encodeToString(
                mapOf(
                    "message" to "Update sponsors.json",
                    "content" to Base64.getEncoder().encodeToString(json.encodeToString(sponsors).toByteArray()),
                    "sha" to currentData.sha
                )
            )

            connection.outputStream.write(payload.toByteArray())
            connection.outputStream.flush()

            if (connection.responseCode in 200..299) {
                return true
            } else {
                println("Failed to update sponsors: ${connection.responseMessage}")
                return false
            }
        } catch (e: Exception) {
            println("Error updating sponsors: ${e.message}")
            return false
        }
    }

    private fun fetchGitHubContent(): GitHubContentResponse? {
        return try {
            val connection = URL(repoApiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $token")
            connection.connect()

            if (connection.responseCode != 200) {
                println("Failed to fetch GitHub content: ${connection.responseMessage}")
                return null
            }

            val response = connection.inputStream.bufferedReader().readText()
            json.decodeFromString(response)
        } catch (e: Exception) {
            println("Error fetching GitHub content: ${e.message}")
            null
        }
    }
}

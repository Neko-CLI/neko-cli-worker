@file:Suppress("SpellCheckingInspection")

package commands.utility

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.json.JSONObject
import utils.NekoCLIApi
import java.awt.Color
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Dependencies : ListenerAdapter() {
    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "dependencies") return

        val query = event.getOption("package")?.asString
        if (query.isNullOrBlank()) {
            event.reply("❌ **Please provide the name of a package or library to search for.**")
                .setEphemeral(true)
                .queue()
            return
        }

        event.deferReply().queue()

        val mavenResult = searchMaven(query)
        val npmResult = searchNpm(query)
        val yarnResult = searchYarn(query)

        val timestamp = Instant.now().atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))

        val embed = EmbedBuilder()
            .setTitle("🔍 Dependency Search Results for `$query`")
            .setDescription("Here are the search results from **Maven**, **NPM**, and **Yarn** repositories.")
            .addField("📦 **Maven**", mavenResult ?: "*No results found.*", false)
            .addField("📦 **NPM**", npmResult ?: "*No results found.*", false)
            .addField("📦 **Yarn**", yarnResult ?: "*No results found.*", false)
            .setFooter("Search performed on $timestamp")
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .build()

        event.hook.editOriginalEmbeds(embed).queue()
    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "dependencies" || event.focusedOption.name != "package") return

        val query = event.focusedOption.value
        val suggestions = searchAutocomplete(query)

        event.replyChoices(suggestions).queue()
    }

    private fun searchAutocomplete(query: String): List<net.dv8tion.jda.api.interactions.commands.Command.Choice> {
        val npmResults = searchNpmAutocomplete(query)
        return npmResults.map { net.dv8tion.jda.api.interactions.commands.Command.Choice(it, it) }
    }

    private fun searchNpmAutocomplete(query: String): List<String> {
        val url = "https://registry.npmjs.org/-/v1/search?text=$query&size=5"
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != 200) return emptyList()

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val packages = json.getJSONArray("objects")
            if (packages.isEmpty) return emptyList()

            (0 until packages.length()).mapNotNull { i ->
                packages.getJSONObject(i).getJSONObject("package").getString("name")
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun searchMaven(query: String): String? {
        val url = "https://search.maven.org/solrsearch/select?q=$query&rows=3&wt=json"
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != 200) return null

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val docs = json.getJSONObject("response").getJSONArray("docs")
            if (docs.isEmpty) return null

            buildString {
                for (i in 0 until docs.length()) {
                    val doc = docs.getJSONObject(i)
                    val groupId = doc.getString("g")
                    val artifactId = doc.getString("a")
                    val version = doc.optString("latestVersion", "Unknown")

                    append("- **`$groupId:$artifactId:$version`**\n")
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun searchNpm(query: String): String? {
        val url = "https://registry.npmjs.org/-/v1/search?text=$query&size=3"
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != 200) return null

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val packages = json.getJSONArray("objects")
            if (packages.isEmpty) return null

            buildString {
                for (i in 0 until packages.length()) {
                    val pkg = packages.getJSONObject(i).getJSONObject("package")
                    val name = pkg.getString("name")
                    val version = pkg.getString("version")
                    val description = pkg.optString("description", "No description")

                    append("- **`$name@$version`**: $description\n")
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun searchYarn(query: String): String? {
        val url = "https://registry.yarnpkg.com/-/v1/search?text=$query&size=3"
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != 200) return null

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val packages = json.getJSONArray("objects")
            if (packages.isEmpty) return null

            buildString {
                for (i in 0 until packages.length()) {
                    val pkg = packages.getJSONObject(i).getJSONObject("package")
                    val name = pkg.getString("name")
                    val version = pkg.getString("version")
                    val description = pkg.optString("description", "No description")

                    append("- **`$name@$version`**: $description\n")
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}

@file:Suppress("SpellCheckingInspection")

package commands.general

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import utils.NekoCLIApi
import java.awt.Color
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class Status : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "status") return

        event.deferReply().queue()

        runBlocking {
            val nekoCliNpmStatus = checkStatus("https://registry.npmjs.org/neko-cli", "NPM")
            val nekoCliYarnStatus = checkStatus("https://yarnpkg.com/package/neko-cli", "Yarn")
            val nekoWebsiteStatus = checkWebsiteStatus("https://neko-cli.com")
            val botStatus = checkBotStatus(event)

            val embed = EmbedBuilder()
                .setTitle("📊 Neko-CLI Status Dashboard")
                .setDescription("Real-time status of Neko-CLI services and bot performance.")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .addField("📦 Neko-CLI on NpmJS", nekoCliNpmStatus, true)
                .addField("📦 Neko-CLI on YarnPkg", nekoCliYarnStatus, true)
                .addField("🌐 Neko-CLI Website", nekoWebsiteStatus, false)
                .addField("🤖 Bot Status", botStatus, false)
                .setThumbnail(event.jda.selfUser.effectiveAvatarUrl)
                .setFooter("Status checked at", event.jda.selfUser.effectiveAvatarUrl)
                .setTimestamp(Instant.now())
                .build()

            event.hook.editOriginalEmbeds(embed).queue()
        }
    }

    private suspend fun checkStatus(url: String, platform: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == 200) {
                "✅ Online ($platform)"
            } else {
                "❌ Offline (Status Code: ${connection.responseCode})"
            }
        } catch (e: Exception) {
            "❌ Error: ${e.message}"
        }
    }

    private suspend fun checkWebsiteStatus(url: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == 200) {
                "✅ Online"
            } else {
                "❌ Offline (Status Code: ${connection.responseCode})"
            }
        } catch (e: Exception) {
            "❌ Error: ${e.message}"
        }
    }

    private fun checkBotStatus(event: SlashCommandInteractionEvent): String {
        val gatewayPing = event.jda.gatewayPing
        return when {
            gatewayPing < 100 -> "✅ Online (Ping: ${gatewayPing}ms)"
            gatewayPing < 200 -> "⚠️ Moderate Latency (Ping: ${gatewayPing}ms)"
            else -> "❌ High Latency (Ping: ${gatewayPing}ms)"
        }
    }
}

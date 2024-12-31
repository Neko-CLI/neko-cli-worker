@file:Suppress("SpellCheckingInspection", "SameParameterValue", "RegExpRedundantEscape")

package commands.general

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import utils.NekoCLIApi
import java.awt.Color
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class Status : ListenerAdapter() {

    private val api = NekoCLIApi()
    private val logFile = File("neko_cli_status.log")

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "status") return

        event.deferReply().queue()

        runBlocking {
            val nekoCliNpmStatus = checkStatus("https://registry.npmjs.org/neko-cli", "NPM")
            val nekoCliYarnStatus = checkStatus("https://yarnpkg.com/package/neko-cli", "Yarn")
            val nekoWebsiteStatus = checkWebsiteStatus("https://neko-cli.com")
            val recentDowntime = analyzeDowntime()
            val botStatus = checkBotStatus(event)

            val embed = EmbedBuilder()
                .setTitle("📊 **Neko-CLI Status Dashboard**")
                .setDescription("Real-time status of **Neko-CLI** services and bot performance.")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .addField("📦 **Neko-CLI on NpmJS**", nekoCliNpmStatus, true)
                .addField("📦 **Neko-CLI on YarnPkg**", nekoCliYarnStatus, true)
                .addField("🌐 **Neko-CLI Website**", nekoWebsiteStatus, false)
                .addField("❗ **Website Downtime (48h)**", recentDowntime, false)
                .addField("🤖 **Bot Status**", botStatus, false)
                .addBlankField(false)
                .addField(
                    "ℹ️ **Details**",
                    "- **NPM**: [View Package](https://www.npmjs.com/package/neko-cli)\n" +
                            "- **Yarn**: [View Package](https://yarnpkg.com/package/neko-cli)\n" +
                            "- **Website**: [Visit Site](https://neko-cli.com)",
                    false
                )
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
                "✅ **Online** ($platform)"
            } else {
                "❌ **Offline** (Status Code: ${connection.responseCode})"
            }
        } catch (e: Exception) {
            "❌ **Error**: ${e.message}"
        }
    }

    private suspend fun checkWebsiteStatus(url: String): String = withContext(Dispatchers.IO) {
        val status = try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == 200) {
                "✅ **Online**"
            } else {
                "❌ **Offline** (Status Code: ${connection.responseCode})"
            }
        } catch (e: Exception) {
            "❌ **Error**: ${e.message}"
        }

        logStatus("Website", status)
        return@withContext status
    }

    private fun checkBotStatus(event: SlashCommandInteractionEvent): String {
        val gatewayPing = event.jda.gatewayPing
        return when {
            gatewayPing < 100 -> "✅ **Online** (Ping: ${gatewayPing}ms)"
            gatewayPing < 200 -> "⚠️ **Moderate Latency** (Ping: ${gatewayPing}ms)"
            else -> "❌ **High Latency** (Ping: ${gatewayPing}ms)"
        }
    }

    private fun logStatus(service: String, status: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        logFile.appendText("[$timestamp] $service: $status\n")
    }

    fun parseDate(dateString: String): LocalDateTime? {
        return try {
            val cleanedDate = dateString.replace(Regex("\\]"), "")
            LocalDateTime.parse(cleanedDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: DateTimeParseException) {
            println("Errore nel parsing della data: ${e.message}")
            null
        }
    }

    private fun analyzeDowntime(): String {
        if (!logFile.exists()) return "No data available."

        val cutoffTime = LocalDateTime.now().minusDays(2)
        val logs = logFile.readLines()
            .mapNotNull {
                val parts = it.split(" ", limit = 3)
                if (parts.size == 3) {
                    val time = parseDate(parts[0].removePrefix("["))
                    val status = parts[2]
                    if (time != null && time.isAfter(cutoffTime)) Pair(time, status) else null
                } else null
            }

        val downtimeCount = logs.count { it.second.contains("Offline", ignoreCase = true) || it.second.contains("Error", ignoreCase = true) }

        return if (downtimeCount > 0) {
            "\u274C **$downtimeCount incidents** of downtime in the last 48 hours."
        } else {
            "\u2705 **No downtime** in the last 48 hours."
        }
    }

}

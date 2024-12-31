@file:Suppress("KotlinConstantConditions", "USELESS_CAST")

package events

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.json.JSONObject
import utils.NekoCLIApi
import java.awt.Color
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class FactOfTheDay(private val api: NekoCLIApi) : ListenerAdapter() {
    private val jda = api.getJdaInstance()

    init {
        val timer = Timer()
        timer.scheduleAtFixedRate(getInitialDelay(), 24 * 60 * 60 * 1000) {
            sendFactOfTheDay()
        }
    }

    private fun getInitialDelay(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance()
        target.set(Calendar.HOUR_OF_DAY, 0)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun sendFactOfTheDay() {
        val fact = fetchRandomFact() ?: return
        val factChannelId = api.getConfig("FACTOFDAYCHANNELID")
        val channel = jda.getTextChannelById(factChannelId) as? TextChannel ?: return

        val guildIcon = channel.guild.iconUrl ?: ""
        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
        val embed = EmbedBuilder()
            .setTitle("🌟 **Fact of the Day**")
            .setDescription(
                "Here is your daily dose of knowledge! Check out today's fact:")
            .addField("📖 **Today's Fact**", "```$fact```", false)
            .addField("🌐 **Source**", "[Geek Jokes API](https://geek-jokes.sameerkumar.website)", true)
            .addField("🕒 **Shared at**", "00:00 UTC", true)
            .setImage(api.getConfig("SERVERIMAGE"))
            .setFooter("Brought to you by ${channel.guild.name} • $timestamp", guildIcon)
            .setThumbnail(guildIcon)
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .build()

        channel.sendMessageEmbeds(embed).queue()
    }

    private fun fetchRandomFact(): String? {
        val apiUrl = "https://geek-jokes.sameerkumar.website/api?format=json"
        return try {
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            if (connection.responseCode != 200) return null

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            json.optString("joke", "No fact available.")
        } catch (e: Exception) {
            println("Error fetching random fact: ${e.message}")
            null
        }
    }
}

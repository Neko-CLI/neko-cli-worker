package apps

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter

class UserInfoApp : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onGenericCommandInteraction(event: GenericCommandInteractionEvent) {
        when (event) {
            is UserContextInteractionEvent -> handleUserContext(event)
        }
    }

    private fun handleUserContext(event: UserContextInteractionEvent) {
        val user: Member? = event.targetMember

        if (event.guild?.id != api.getConfig("GUILDID")) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Error: Incorrect Server")
                    .setDescription("You are not in the correct server to use this command.")
                    .setImage(api.getConfig("SERVERIMAGE"))
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (user == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ Error: User Not Found")
                    .setDescription("Unable to retrieve the user information.")
                    .setImage(api.getConfig("SERVERIMAGE"))
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val accountCreated = user.timeCreated.format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
        val joinedServer = user.timeJoined.format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))

        val roles = user.roles.joinToString { "`${it.name}`" }
        val permissions = user.permissions.joinToString { "`${it.name}`" }

        val embed = EmbedBuilder()
            .setTitle("👤 User Information")
            .setDescription("Here are the details about ${user.asMention}.")
            .addField("🆔 User ID", user.id, true)
            .addField("👤 Username", user.user.name, true)
            .addField("#️⃣ Discriminator", "#${user.user.discriminator}", true)
            .addField("📎 Mention", user.asMention, true)
            .addField("🖼️ Avatar URL", "[Click Here](${user.user.avatarUrl ?: "No Avatar"})", true)
            .addField("📅 Account Created", accountCreated, true)
            .addField("📅 Joined Server", joinedServer, true)
            .addField("🛡️ Roles", truncateContent(roles), false)
            .addField("🔒 Permissions", truncateContent(permissions), false)
            .setThumbnail(user.user.avatarUrl)
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
            .setFooter("Requested by ${event.user.name}", event.user.avatarUrl)
            .setTimestamp(Instant.now())
            .build()

        event.replyEmbeds(embed).queue()
    }

    private fun truncateContent(content: String): String {
        return if (content.length > 1024) {
            content.take(1020) + "..."
        } else {
            content
        }
    }
}

package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Depex : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val password: String? = event.getOption("password", OptionMapping::getAsString)
        val user: Member? = event.getOption("user", OptionMapping::getAsMember)
        val role: Role? = event.getOption("role", OptionMapping::getAsRole)

        if (event.name != "depex") return

        if (event.guild?.id != api.getConfig("GUILDID")) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Access Denied**")
                    .setDescription(
                        "You are not in the correct server to use this command.\n\n" +
                                "📌 **Tip:** Ensure you are using this command in the authorized server."
                    )
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (!api.isUnStackss(event.member?.id.toString())) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("🔒 **Permission Denied**")
                    .setDescription(
                        "You do not have the required permissions to use this command.\n\n" +
                                "🔑 **Required Role:** Ensure you are listed in the authorized personnel."
                    )
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (user == null || role == null) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Missing Arguments**")
                    .setDescription(
                        "You must specify both a **user** and a **role** to execute this command.\n\n" +
                                "📌 **Usage:** `/depex user:<@User> role:<@Role> password:<Password>`"
                    )
                    .setColor(Color.YELLOW)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (!event.member!!.canInteract(user)) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("🔒 **Interaction Denied**")
                    .setDescription(
                        "You cannot interact with this user due to **role hierarchy restrictions**.\n\n" +
                                "🔒 **Tip:** Ensure your role is higher than the target user's role."
                    )
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (password != api.getConfig("PASSWORD")) {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("🔑 **Incorrect Password**")
                    .setDescription(
                        "The password you provided is incorrect.\n\n" +
                                "📌 **Tip:** Double-check the password and try again."
                    )
                    .setColor(Color.RED)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .setTimestamp(Instant.now())
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))

        if (user.roles.contains(role)) {
            api.removeRole(event.jda, api.getConfig("GUILDID"), user, role.id)

            event.hook.editOriginalEmbeds(
                EmbedBuilder()
                    .setTitle("✅ **Permission Revoked**")
                    .setDescription(
                        "The role **${role.name}** has been successfully removed from ${user.asMention} by ${event.member!!.asMention}."
                    )
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .addField("👤 **User**", user.asMention, true)
                    .addField("🛡️ **Role Removed**", role.name, true)
                    .addField("⏰ **Timestamp**", timestamp, false)
                    .setTimestamp(Instant.now())
                    .build()
            ).queue()
        } else {
            event.hook.editOriginalEmbeds(
                EmbedBuilder()
                    .setTitle("⚠️ **Role Not Found**")
                    .setDescription(
                        "The user ${user.asMention} does not have the role **${role.name}**."
                    )
                    .setColor(Color.YELLOW)
                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                    .addField("👤 **User**", user.asMention, true)
                    .addField("🛡️ **Role**", role.name, true)
                    .addField("⏰ **Timestamp**", timestamp, false)
                    .setTimestamp(Instant.now())
                    .build()
            ).queue()
        }

        AnsiConsole.systemInstall()
        println(
            ansi().fgBrightBlue().a("[").reset().a("NekoCLIWorker").fgBrightBlue().a("]").reset()
                .a(" Depexed ${user.user.name}#${user.user.discriminator} (${user.id}) from role ${role.name} (${role.id}) at $timestamp.").reset()
        )
    }
}

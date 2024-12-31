@file:Suppress("SpellCheckingInspection")

package commands.general

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.FileUpload
import utils.NekoCLIApi
import java.awt.Color
import java.net.URL

class Announce : ListenerAdapter() {

    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val announcement: String? = event.getOption("announce", OptionMapping::getAsString)
        val imageUrl: String? = event.getOption("image", OptionMapping::getAsString)
        val fileUrl: String? = event.getOption("file", OptionMapping::getAsString)
        val links: String? = event.getOption("links", OptionMapping::getAsString)

        if (event.name == "announce") {
            val guild = event.guild
            if (guild?.id != api.getConfig("GUILDID")) {
                event.replyEmbeds(
                    EmbedBuilder()
                        .setTitle("❌ Access Denied")
                        .setDescription(
                            "You are not in the correct server to use this command. Please ensure you have access to the appropriate server."
                        )
                        .setColor(Color.RED)
                        .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                        .setTimestamp(event.timeCreated)
                        .build()
                ).setEphemeral(true).queue()
                return
            }

            if (announcement.isNullOrBlank()) {
                event.replyEmbeds(
                    EmbedBuilder()
                        .setTitle("⚠️ Missing Announcement Content")
                        .setDescription(
                            "You need to provide announcement content in the `announce` option. Use the appropriate format for your message."
                        )
                        .setColor(Color.RED)
                        .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                        .setTimestamp(event.timeCreated)
                        .build()
                ).setEphemeral(true).queue()
                return
            }

            val announcementChannelId = api.getConfig("ANNOUNCEMENTCHANNELID")
            val announcementChannel = guild.getNewsChannelById(announcementChannelId)

            if (announcementChannel == null) {
                event.replyEmbeds(
                    EmbedBuilder()
                        .setTitle("❌ Error")
                        .setDescription(
                            "The announcement channel could not be found. Please verify the configuration or contact an administrator."
                        )
                        .setColor(Color.RED)
                        .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                        .setTimestamp(event.timeCreated)
                        .build()
                ).setEphemeral(true).queue()
                return
            }

            val embedBuilder = EmbedBuilder()
                .setTitle("📢 New Announcement")
                .setDescription(announcement)
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setAuthor("Neko-CLI Worker", api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                .setTimestamp(event.timeCreated)
                .setFooter("Announcement sent by ${event.user.name}", event.user.effectiveAvatarUrl)

            if (!imageUrl.isNullOrBlank()) {
                embedBuilder.setImage(imageUrl)
            }

            if (!links.isNullOrBlank()) {
                embedBuilder.addField("🔗 Links", links, false)
            }

            if (!fileUrl.isNullOrBlank()) {
                try {
                    val fileUpload = FileUpload.fromData(URL(fileUrl).openStream(), fileUrl.substringAfterLast("/"))
                    announcementChannel.sendMessageEmbeds(embedBuilder.build())
                        .addFiles(fileUpload)
                        .queue({
                            event.replyEmbeds(
                                EmbedBuilder()
                                    .setTitle("✅ Announcement Sent")
                                    .setDescription("The announcement has been successfully delivered to the specified channel.")
                                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                                    .setTimestamp(event.timeCreated)
                                    .build()
                            ).setEphemeral(true).queue()
                        }, {
                            event.replyEmbeds(
                                EmbedBuilder()
                                    .setTitle("❌ Error")
                                    .setDescription("Failed to send the announcement with the file attached. Ensure the URL is correct.")
                                    .setColor(Color.RED)
                                    .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                                    .setTimestamp(event.timeCreated)
                                    .build()
                            ).setEphemeral(true).queue()
                        })
                } catch (_: Exception) {
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("❌ Invalid File URL")
                            .setDescription("The provided file URL could not be processed. Please check the URL and try again.")
                            .setColor(Color.RED)
                            .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                            .setTimestamp(event.timeCreated)
                            .build()
                    ).setEphemeral(true).queue()
                }
            } else {
                announcementChannel.sendMessageEmbeds(embedBuilder.build()).queue({
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("✅ Announcement Sent")
                            .setDescription("The announcement has been successfully delivered to the specified channel.")
                            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                            .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                            .setTimestamp(event.timeCreated)
                            .build()
                    ).setEphemeral(true).queue()
                }, {
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle("❌ Error")
                            .setDescription("Failed to send the announcement. Ensure you have the proper permissions.")
                            .setColor(Color.RED)
                            .setAuthor(event.jda.selfUser.name, api.getConfig("WEBSITE"), event.jda.selfUser.avatarUrl)
                            .setTimestamp(event.timeCreated)
                            .build()
                    ).setEphemeral(true).queue()
                })
            }
        }
    }
}

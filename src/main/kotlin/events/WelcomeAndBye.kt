@file:Suppress("USELESS_ELVIS", "SpellCheckingInspection", "unused")

package events

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import utils.NekoCLIApi
import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

class WelcomeAndBye : ListenerAdapter() {
    private val api = NekoCLIApi()
    private val welcomeChannelId = api.getConfig("WELCOMECHANNELID")
    private val unverifiedRoleId = api.getConfig("UNVERIFIEDROLEID")

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val member = event.member ?: return
        val guild = event.guild
        val unverifiedRole = guild.getRoleById(unverifiedRoleId)

        unverifiedRole?.let {
            guild.addRoleToMember(member, it).queue {
                AnsiConsole.systemInstall()
                println(
                    Ansi.ansi()
                        .fgBrightBlue().a("[Success] ")
                        .reset().a("Unverified role successfully assigned to ")
                        .fgBrightBlue().a(member.user.name)
                        .reset()
                )
            }
        } ?: run {
            AnsiConsole.systemInstall()
            println(
                Ansi.ansi()
                    .fgBrightRed().a("[Error] ")
                    .reset().a("Unverified role not found!")
                    .reset()
            )
        }

        val welcomeImage = generateWelcomeImage(member, "🎉 Welcome to the server! 🎉")
        val welcomeChannel = event.guild.getTextChannelById(welcomeChannelId)

        val embed = EmbedBuilder()
            .setTitle("🌟 Welcome, ${member.user.name}!")
            .setDescription(
                "👋 Hello ${member.asMention}, we’re glad to have you here!\n\n" +
                        "📜 Please check out our rules and feel free to introduce yourself in the appropriate channel."
            )
            .setThumbnail(member.user.effectiveAvatarUrl)
            .setImage("attachment://welcome.png")
            .setColor(Color(36, 115, 245))
            .setFooter("User joined at ${Instant.now()}")
            .build()

        welcomeChannel?.sendMessageEmbeds(embed)
            ?.addFiles(FileUpload.fromData(welcomeImage, "welcome.png"))
            ?.queue()
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val member = event.member ?: return
        val goodbyeImage = generateWelcomeImage(member, "Goodbye! We’ll miss you! 👋")
        val welcomeChannel = event.guild.getTextChannelById(welcomeChannelId)

        val embed = EmbedBuilder()
            .setTitle("😢 Goodbye, ${member.user.name}")
            .setDescription(
                "We’re sad to see you go, ${member.user.name}.\n\n" +
                        "💌 Feel free to come back anytime. Take care!"
            )
            .setThumbnail(member.user.effectiveAvatarUrl)
            .setImage("attachment://goodbye.png")
            .setColor(Color(200, 20, 20))
            .setFooter("User joined at " + Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")))
            .build()

        welcomeChannel?.sendMessageEmbeds(embed)
            ?.addFiles(FileUpload.fromData(goodbyeImage, "goodbye.png"))
            ?.queue()
    }

    private fun generateWelcomeImage(member: Member, message: String): ByteArray {
        val avatarUrl = member.user.effectiveAvatarUrl + "?size=512"
        val avatarImage = ImageIO.read(java.net.URL(avatarUrl))
        val width = 1000
        val height = 500
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.color = Color(17, 24, 39)
        graphics.fillRect(0, 0, width, height)

        val particleColors = listOf(Color(36, 115, 245, 150), Color(50, 150, 250, 100), Color(20, 80, 200, 120))
        for (i in 0..100) {
            graphics.color = particleColors.random()
            val particleSize = (Math.random() * 10 + 5).toInt()
            val x = (Math.random() * width).toInt()
            val y = (Math.random() * height).toInt()
            graphics.fill(Ellipse2D.Double(x.toDouble(), y.toDouble(), particleSize.toDouble(), particleSize.toDouble()))
        }

        val avatarSize = 200
        val avatarClip = RoundRectangle2D.Double(50.0, 150.0, avatarSize.toDouble(), avatarSize.toDouble(), 100.0, 100.0)
        graphics.clip = avatarClip
        graphics.drawImage(avatarImage, 50, 150, avatarSize, avatarSize, null)
        graphics.clip = null
        graphics.color = Color(36, 115, 245)
        graphics.stroke = BasicStroke(5f)
        graphics.draw(avatarClip)

        graphics.font = Font("Arial", Font.BOLD, 50)
        graphics.color = Color(36, 115, 245)
        graphics.drawString(member.user.name, 300, 220)

        graphics.font = Font("Arial", Font.PLAIN, 30)
        graphics.color = Color(36, 115, 245)
        graphics.drawString(message, 300, 280)

        graphics.stroke = BasicStroke(2f)
        graphics.color = Color(36, 115, 245)
        graphics.drawLine(300, 300, width - 100, 300)

        graphics.dispose()

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        return outputStream.toByteArray()
    }
}
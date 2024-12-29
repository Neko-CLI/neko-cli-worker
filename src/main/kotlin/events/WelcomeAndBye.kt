@file:Suppress("USELESS_ELVIS", "SpellCheckingInspection", "Unused")

package events

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload
import utils.NekoCLIApi
import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class WelcomeAndBye : ListenerAdapter() {
    private val api = NekoCLIApi()
    private val welcomeChannelId = api.getConfig("WELCOMECHANNELID")
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val member = event.member ?: return
        val welcomeImage = generateWelcomeImage(member, "Welcome to the server!")
        val welcomeChannel = event.guild.getTextChannelById(welcomeChannelId)
        welcomeChannel?.sendMessage("Welcome ${member.asMention} to the server!")
            ?.addFiles(FileUpload.fromData(welcomeImage, "welcome.png"))
            ?.queue()
    }
    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val member = event.member ?: return
        val goodbyeImage = generateWelcomeImage(member, "Goodbye! We'll miss you!")
        val welcomeChannel = event.guild.getTextChannelById(welcomeChannelId)

        welcomeChannel?.sendMessage("Goodbye ${member.user.asTag}! We hope to see you again!")
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

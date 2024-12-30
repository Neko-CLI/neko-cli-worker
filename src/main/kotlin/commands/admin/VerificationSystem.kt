@file:Suppress("SpellCheckingInspection")

package commands.admin

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import utils.NekoCLIApi
import java.awt.Color

class VerificationSystem : ListenerAdapter() {

    private val verificationCodes = mutableMapOf<String, String>()
    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "setverificationchannel") return

        val embed = EmbedBuilder()
            .setTitle("✅ Verification Required")
            .setDescription(
                "To access the server, verify your account by following these steps:\n\n" +
                        "1. Click **📨 Send Code** to receive a unique verification code in your direct messages.\n" +
                        "2. Click **🔐 Verify Code** and enter the code to complete the verification."
            )
            .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
            .setThumbnail(event.jda.selfUser.effectiveAvatarUrl)
            .setFooter("Verification System", event.jda.selfUser.effectiveAvatarUrl)
            .build()

        val sendCodeButton = Button.primary("send-code", "📨 Send Code")
        val verifyCodeButton = Button.success("verify-code", "🔐 Verify Code")

        event.replyEmbeds(
            EmbedBuilder()
                .setTitle("✅ Success")
                .setDescription("The verification message has been sent to this channel.")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .build()
        )
            .setEphemeral(true)
            .queue()

        event.channel.sendMessageEmbeds(embed).setActionRow(sendCodeButton, verifyCodeButton).queue()
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when (event.componentId) {
            "send-code" -> handleSendCode(event)
            "verify-code" -> handleVerifyCode(event)
        }
    }

    private fun handleSendCode(event: ButtonInteractionEvent) {
        val userId = event.user.id
        val verificationCode = generateVerificationCode()

        verificationCodes[userId] = verificationCode

        event.user.openPrivateChannel().queue(
            { channel ->
                val embed = EmbedBuilder()
                    .setTitle("🔐 Verification Code")
                    .setDescription(
                        "Here is your unique verification code:\n\n" +
                                "**```$verificationCode```**\n\n" +
                                "Return to the server and use the **🔐 Verify Code** button to complete the verification."
                    )
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .setFooter("Verification System", event.jda.selfUser.effectiveAvatarUrl)
                    .build()

                channel.sendMessageEmbeds(embed).queue(
                    {
                        event.replyEmbeds(
                            EmbedBuilder()
                                .setTitle("📨 Code Sent")
                                .setDescription("A verification code has been sent to your direct messages.")
                                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                                .build()
                        )
                            .setEphemeral(true)
                            .queue()
                    },
                    {
                        event.replyEmbeds(
                            EmbedBuilder()
                                .setTitle("❌ Error")
                                .setDescription("Unable to send you a private message. Please enable direct messages and try again.")
                                .setColor(Color.RED)
                                .build()
                        )
                            .setEphemeral(true)
                            .queue()
                    }
                )
            },
            {
                event.replyEmbeds(
                    EmbedBuilder()
                        .setTitle("❌ Error")
                        .setDescription("Unable to open a private channel. Please enable direct messages and try again.")
                        .setColor(Color.RED)
                        .build()
                )
                    .setEphemeral(true)
                    .queue()
            }
        )
    }

    private fun handleVerifyCode(event: ButtonInteractionEvent) {
        val modal = Modal.create("verify-modal", "🔐 Enter Verification Code")
            .addActionRow(
                TextInput.create("verification-code", "Verification Code", TextInputStyle.SHORT)
                    .setPlaceholder("Enter the code you received in direct messages")
                    .setRequired(true)
                    .build()
            )
            .build()

        event.replyModal(modal).queue()
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != "verify-modal") return

        val enteredCode = event.getValue("verification-code")?.asString ?: return
        val userId = event.user.id
        val storedCode = verificationCodes[userId]

        if (storedCode != null && storedCode == enteredCode) {
            verificationCodes.remove(userId)
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("✅ **Verification Successful**")
                    .setDescription("You have been successfully verified and granted access to the server.")
                    .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                    .build()
            )
                .setEphemeral(true)
                .queue()

            val guild = event.guild
            if (guild == null) {
                println("[Error] Guild is null. Ensure the interaction occurs in a server context.")
                return
            }

            val member = guild.retrieveMember(event.user).complete()
            if (member == null) {
                println("[Error] Member is null. Ensure the user is part of the server.")
                return
            }

            val verifiedRole = guild.getRoleById(api.getConfig("VERIFICATIONROLEID"))
            val unverifiedRole = guild.getRoleById(api.getConfig("UNVERIFIEDROLEID"))

            if (verifiedRole != null) {
                guild.addRoleToMember(member, verifiedRole).queue({
                    println("[Info] Successfully added role '${verifiedRole.name}' to user '${member.user.name}'.")
                }, {
                    println("[Error] Failed to add role '${verifiedRole.name}' to user '${member.user.name}': ${it.message}")
                })
            } else {
                println("[Error] Verified role not found. Ensure the ID is correct in the configuration.")
            }

            if (unverifiedRole != null) {
                guild.removeRoleFromMember(member, unverifiedRole).queue({
                    println("[Info] Successfully removed role '${unverifiedRole.name}' from user '${member.user.name}'.")
                }, {
                    println("[Error] Failed to remove role '${unverifiedRole.name}' from user '${member.user.name}': ${it.message}")
                })
            } else {
                println("[Error] Unverified role not found. Ensure the ID is correct in the configuration.")
            }
        } else {
            event.replyEmbeds(
                EmbedBuilder()
                    .setTitle("❌ **Verification Failed**")
                    .setDescription("The code you entered is incorrect. Please try again.")
                    .setColor(Color.RED)
                    .build()
            )
                .setEphemeral(true)
                .queue()
        }
    }



    private fun generateVerificationCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}

package events

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import utils.NekoCLIApi
import java.awt.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class ModalAskHR : EventListener {

    private val api = NekoCLIApi()
    private val questionMappings = mutableMapOf<String, Pair<User, String>>()

    override fun onEvent(event: GenericEvent) {
        try {
            when (event) {
                is ButtonInteractionEvent -> handleButtonInteraction(event)
                is ModalInteractionEvent -> handleModalInteraction(event)
            }
        } catch (e: Exception) {
            println("Error handling event: ${e.message}")
        }
    }

    private fun handleButtonInteraction(event: ButtonInteractionEvent) {
        when {
            event.componentId == "sqthr" -> handleAskHRButton(event)
            event.componentId.startsWith("reply_") -> handleReplyButton(event)
        }
    }

    private fun handleModalInteraction(event: ModalInteractionEvent) {
        when {
            event.modalId == "sqthr" -> handleAskHRModal(event)
            event.modalId.startsWith("reply_") -> handleReplyModal(event)
        }
    }

    private fun handleAskHRButton(event: ButtonInteractionEvent) {
        val modal = Modal.create("sqthr", "Ask HR Your Questions 🙋‍♂️")
            .addComponents(
                ActionRow.of(
                    TextInput.create("name", "Your Name ✨", TextInputStyle.SHORT)
                        .setPlaceholder("Enter your full name (4-25 characters)")
                        .setRequired(true)
                        .setMinLength(4)
                        .setMaxLength(25)
                        .build()
                ),
                ActionRow.of(
                    TextInput.create("email", "Your Email 📬", TextInputStyle.SHORT)
                        .setPlaceholder("Enter your contact email (e.g., name@example.com)")
                        .setRequired(true)
                        .setMinLength(3)
                        .setMaxLength(320)
                        .build()
                ),
                ActionRow.of(
                    TextInput.create("question", "Your Question for HR 🏢", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Write your question for HR (at least 8 characters)")
                        .setRequired(true)
                        .setMinLength(8)
                        .setMaxLength(500)
                        .build()
                )
            )
            .build()
        event.replyModal(modal).queue()
    }

    private fun handleAskHRModal(event: ModalInteractionEvent) {
        val name = event.getValue("name")?.asString ?: return
        val email = event.getValue("email")?.asString ?: return
        val question = event.getValue("question")?.asString ?: return
        val questionId = UUID.randomUUID().toString()

        val hrqChannelId = api.getConfig("HRQUESTIONS")
        val hrqChannel = event.jda.getTextChannelById(hrqChannelId)

        if (hrqChannel != null) {
            val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
            val embed = EmbedBuilder()
                .setTitle("New HR Question 🏢")
                .setDescription("A new question has been submitted by ${event.user.asMention}")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .addField("User Name 📝", name, true)
                .addField("Discord Tag 🔖", event.user.name, true)
                .addField("Contact Email 📧", email, true)
                .addField("Question ❓", question, false)
                .addField("Question ID 🔑", questionId, true)
                .addField("Submitted At 🕒", timestamp, false)
                .setTimestamp(Instant.now())
                .build()

            hrqChannel.sendMessageEmbeds(embed)
                .addActionRow(Button.primary("reply_$questionId", "Reply").withEmoji(Emoji.fromUnicode("✉️")))
                .queue { message ->
                    questionMappings[questionId] = event.user to message.id
                    println("Question mapped: questionId=$questionId, user=${event.user.name}, messageId=${message.id}")
                }
        }
        event.reply("👍 Thank you for your question! Our HR team will get back to you as soon as possible.")
            .setEphemeral(true)
            .queue()
    }

    private fun handleReplyButton(event: ButtonInteractionEvent) {
        val questionId = event.componentId.substringAfter("reply_")
        if (!questionMappings.containsKey(questionId)) {
            event.reply("❌ Question not found or already answered.")
                .setEphemeral(true)
                .queue()
            return
        }

        val replyModal = Modal.create("reply_$questionId", "Reply to Question")
            .addComponents(
                ActionRow.of(
                    TextInput.create("response", "Your Response ✍️", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Write your response here")
                        .setRequired(true)
                        .setMinLength(8)
                        .setMaxLength(500)
                        .build()
                )
            )
            .build()
        event.replyModal(replyModal).queue()
    }

    private fun handleReplyModal(event: ModalInteractionEvent) {
        val questionId = event.modalId.substringAfter("reply_")
        val response = event.getValue("response")?.asString ?: return
        val (user, messageId) = questionMappings.remove(questionId) ?: run {
            event.reply("❌ Question not found or already answered.")
                .setEphemeral(true)
                .queue()
            return
        }

        println("Processing reply: questionId=$questionId, user=${user.name}, messageId=$messageId")

        user.openPrivateChannel().queue({ channel ->
            val embed = EmbedBuilder()
                .setTitle("Your Question has been Answered! 📨")
                .setDescription("We have provided a response to your question.")
                .addField("📌 Question ID", questionId, false)
                .addField("📝 Response", response, false)
                .addField("👤 Answered By", event.user.name, true)
                .addField("⏰ Answered At", Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")), true)
                .setFooter("For more info, visit our website or live chat support.")
                .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                .setTimestamp(Instant.now())
                .build()

            channel.sendMessageEmbeds(embed).queue()
        }, {
            println("Failed to send DM to user: ${user.name}")
            event.reply("❌ Unable to send a private message to the user. They may have DMs disabled.")
                .setEphemeral(true)
                .queue()
        })

        val hrqChannelId = api.getConfig("HRQUESTIONS")
        val hrqChannel = event.jda.getTextChannelById(hrqChannelId)

        hrqChannel?.retrieveMessageById(messageId)?.queue({ message ->
            val updatedEmbed = EmbedBuilder(message.embeds[0])
                .setFooter("Answered by ${event.user.name}")
                .build()
            message.editMessageEmbeds(updatedEmbed)
                .setComponents(
                    ActionRow.of(
                        Button.secondary("answered_$questionId", "Answered by ${event.user.name}").asDisabled()
                    )
                ).queue()
        }, {
            println("Failed to update the message in HR channel: messageId=$messageId")
            event.reply("❌ Unable to update the original question message.")
                .setEphemeral(true)
                .queue()
        })

        event.reply("✅ Response sent to the user and question marked as answered.")
            .setEphemeral(true)
            .queue()
    }
}

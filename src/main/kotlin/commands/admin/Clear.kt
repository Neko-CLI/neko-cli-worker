@file:Suppress("KotlinConstantConditions")

package commands.admin

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import utils.NekoCLIApi
import java.awt.Color
import java.time.OffsetDateTime

class Clear : ListenerAdapter() {
    private val api = NekoCLIApi()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "clear") return
        if (!event.member?.hasPermission(Permission.ADMINISTRATOR)!!) {
            event.replyEmbeds(
                net.dv8tion.jda.api.EmbedBuilder()
                    .setTitle("🚫 **Insufficient Permissions**")
                    .setDescription("❌ You do not have permission to use this command.\n\n🔑 **Required Permission:** `Administrator`.")
                    .setColor(Color.RED)
                    .setFooter("Permission check performed", event.jda.selfUser.avatarUrl)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        val amount = event.getOption("amount")?.asInt ?: 0
        if (amount <= 0 || amount > 1000) {
            event.replyEmbeds(
                net.dv8tion.jda.api.EmbedBuilder()
                    .setTitle("⚠️ **Invalid Input**")
                    .setDescription("❌ Please provide a valid number of messages to delete (1-1000).")
                    .setColor(Color.YELLOW)
                    .setFooter("Validation error", event.jda.selfUser.avatarUrl)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        if (event.channel is GuildMessageChannel) {
            val channelName = event.channel.name
            event.replyEmbeds(
                net.dv8tion.jda.api.EmbedBuilder()
                    .setTitle("⏳ **Processing Request**")
                    .setDescription("Deleting **$amount** messages from `#$channelName`. Please wait...")
                    .setColor(Color.YELLOW)
                    .setFooter("Message deletion in progress", event.jda.selfUser.avatarUrl)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()

            deleteMessagesInBatches(event.channel as GuildMessageChannel, amount) {
                event.hook.editOriginalEmbeds(
                    net.dv8tion.jda.api.EmbedBuilder()
                        .setTitle("✅ **Success**")
                        .setDescription("Successfully deleted **$amount** messages in `#$channelName`!")
                        .setColor(Color.decode(api.getConfig("WORKERCOLOR")))
                        .setFooter("Operation completed", event.jda.selfUser.avatarUrl)
                        .setTimestamp(OffsetDateTime.now())
                        .build()
                ).queue()
            }
        } else {
            event.replyEmbeds(
                net.dv8tion.jda.api.EmbedBuilder()
                    .setTitle("❌ **Unsupported Channel**")
                    .setDescription("This command cannot be used in this type of channel. Please use it in a text channel.")
                    .setColor(Color.RED)
                    .setFooter("Channel type not supported", event.jda.selfUser.avatarUrl)
                    .setTimestamp(event.timeCreated)
                    .build()
            ).setEphemeral(true).queue()
        }
    }

    private fun deleteMessagesInBatches(
        channel: GuildMessageChannel,
        amount: Int,
        onComplete: () -> Unit
    ) {
        val twoWeeksAgo = OffsetDateTime.now().minusWeeks(2)
        val batches = amount / 100 + if (amount % 100 > 0) 1 else 0
        var remaining = amount

        fun deleteBatch(batchIndex: Int) {
            if (batchIndex >= batches || remaining <= 0) {
                onComplete()
                return
            }
            val toRetrieve = if (remaining > 100) 100 else remaining
            channel.history.retrievePast(toRetrieve).queue { messages ->
                if (messages.isEmpty()) {
                    onComplete()
                    return@queue
                }

                val (deletable, tooOld) = messages.partition { it.timeCreated.isAfter(twoWeeksAgo) }

                if (deletable.isNotEmpty()) {
                    channel.deleteMessages(deletable).queue({
                        remaining -= deletable.size
                        deleteBatch(batchIndex + 1)
                    }, {
                        onComplete()
                    })
                }

                tooOld.forEach { oldMessage ->
                    oldMessage.delete().queue(null, null)
                }

                if (tooOld.isNotEmpty()) {
                    remaining -= tooOld.size
                }

                if (deletable.isEmpty() && tooOld.isEmpty()) {
                    onComplete()
                }
            }
        }

        deleteBatch(0)
    }
}

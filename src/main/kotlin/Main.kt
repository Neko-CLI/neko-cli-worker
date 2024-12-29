@file:Suppress("unused", "SpellCheckingInspection")

import commands.admin.*
import commands.general.*
import commands.utility.Dependencies
import commands.utility.SnapCode
import commands.utility.UserInfo
import events.ModalAskHR
import events.Ready
import events.WelcomeAndBye
import events.WorkerErrors
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import utils.NekoCLIApi

fun main() {
    val api = NekoCLIApi()
    api.createResourcesFolder()
    val nekoCLIBuilder = JDABuilder.createDefault(api.getEnv("TOKEN"))
    fun configureMemoryUsage() {
        nekoCLIBuilder.disableCache(
            CacheFlag.MEMBER_OVERRIDES,
            CacheFlag.ACTIVITY,
            CacheFlag.EMOJI,
            CacheFlag.STICKER,
            CacheFlag.SCHEDULED_EVENTS,
            CacheFlag.VOICE_STATE
        )
        nekoCLIBuilder.setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER))
        nekoCLIBuilder.setChunkingFilter(ChunkingFilter.NONE)
        nekoCLIBuilder.enableIntents(
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT
        )
        nekoCLIBuilder.enableIntents(
            GatewayIntent.entries
        )
        nekoCLIBuilder.setLargeThreshold(100)
    }
    fun components() {
        nekoCLIBuilder.setBulkDeleteSplittingEnabled(false)
    }
    fun events() {
        nekoCLIBuilder.addEventListeners(
            Ready(),
            WorkerErrors(),
            ModalAskHR(),
            WelcomeAndBye(),
        )
    }
    fun commands() {
        nekoCLIBuilder.addEventListeners(
            Ban(),
            StopBot(),
            Pex(),
            Depex(),
            Announce(),
            Help(),
            UserInfo(),
            SetAskToHRModal(),
            Sponsors(),
            VerificationSystem(),
            TicketForums(),
            Status(),
            PassGen(),
            Clear(),
            Suggest(),
            BugReport(),
            Dependencies(),
            SnapCode(),
        )
    }
    fun build() {
        configureMemoryUsage()
        components()
        events()
        commands()
        nekoCLIBuilder.build().awaitReady().updateCommands().addCommands(
            Commands.slash("ban", "🔨 Ban a user from the server.")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                .setGuildOnly(true)
                .addOption(OptionType.USER, "user", "👤 The user to ban.", true)
                .addOption(OptionType.STRING, "reason", "📝 Reason for the ban."),
            Commands.slash("stopbot", "🛑 Shutdown the bot.")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.STRING, "password", "🔒 Password for authorization.", true),
            Commands.slash("pex", "✨ Grant a permission role to a user.")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.USER, "user", "👤 The user to grant the role to.", true)
                .addOption(OptionType.ROLE, "role", "🛡️ The role to grant.", true)
                .addOption(OptionType.STRING, "password", "🔒 Password for authorization.", true),
            Commands.slash("depex", "❌ Revoke a permission role from a user.")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.USER, "user", "👤 The user to revoke the role from.", true)
                .addOption(OptionType.ROLE, "role", "🛡️ The role to revoke.", true)
                .addOption(OptionType.STRING, "password", "🔒 Password for authorization.", true),
            Commands.slash("announce", "📢 Send an announcement to the server.")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOptions(
                    OptionData(OptionType.STRING, "announce", "📝 The content of the announcement.", true),
                    OptionData(OptionType.STRING, "image", "🖼️ URL of the image to include in the announcement.", false),
                    OptionData(OptionType.STRING, "file", "📁 URL of the file to include in the announcement.", false),
                    OptionData(OptionType.STRING, "links", "🔗 Additional links to include in the announcement.", false)
                ),
            Commands.slash("help", "❓ Show the help menu 🍖.")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_SEND)),
            Commands.slash("userinfo", "👤 Get information about a user.")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.USER, "user", "👤 The user to retrieve info about.", true),
            Commands.slash("setasktohrmodal", "🙋‍♂️ Configure and Enable the Ask to HR Modal for Questions")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
            Commands.slash("sponsor", "✨ Manage and showcase your sponsors with style! 🎉")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommandGroups(
                    net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData("manage", "⚙️ Sponsor management made easy!")
                        .addSubcommands(
                            net.dv8tion.jda.api.interactions.commands.build.SubcommandData("list", "📋 View all your amazing sponsors!")
                                .setDescription("📋 List and explore the details of all your sponsors."),
                            net.dv8tion.jda.api.interactions.commands.build.SubcommandData("add", "➕ Add a sponsor to the list!")
                                .setDescription("➕ Add a new sponsor with all the details you want!")
                                .addOption(OptionType.STRING, "name", "🏷️ Name of the sponsor", true)
                                .addOption(OptionType.STRING, "imagelink", "🖼️ Image URL for the sponsor logo", true)
                                .addOption(OptionType.STRING, "link", "🔗 Website URL of the sponsor", true)
                                .addOption(OptionType.STRING, "color", "🎨 Hexadecimal color code (e.g., #FFFFFF)", false)
                                .addOption(OptionType.BOOLEAN, "icononly", "🖼️ Show only the icon?", false)
                                .addOption(OptionType.INTEGER, "iconsize", "📏 Size of the icon (in pixels)", false),
                            net.dv8tion.jda.api.interactions.commands.build.SubcommandData("remove", "➖ Remove a sponsor!")
                                .setDescription("➖ Remove an existing sponsor from the list.")
                                .addOption(OptionType.STRING, "name", "🏷️ Name of the sponsor to remove", true)
                        )
                ),
            Commands.slash("setverificationchannel", "🔐 Set up the advanced verification system in the current channel 🚀")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
            Commands.slash("setticketforum", "🎟️ Set up a ticket system for users to create private threads for support.")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
            Commands.slash("status", "📡 Check the status of Neko-CLI, its website, and the bot!"),
            Commands.slash("passgen", "🔐 Generate your custom secure password with style! 🌟")
                .addOptions(
                    OptionData(OptionType.INTEGER, "maxchar", "🖊️ Maximum length of the password (up to 1000)", false),
                    OptionData(OptionType.BOOLEAN, "uppercase", "🔤 Include uppercase letters", false),
                    OptionData(OptionType.BOOLEAN, "lowercase", "🔡 Include lowercase letters", false),
                    OptionData(OptionType.BOOLEAN, "numbers", "🔢 Include numbers", false),
                    OptionData(OptionType.BOOLEAN, "symbols", "✨ Include symbols", false),
                    OptionData(OptionType.STRING, "type", "🎭 Choose character type", false)
                        .addChoice("Easy to say", "Easy to say")
                        .addChoice("Easy to read", "Easy to read")
                        .addChoice("All characters", "All characters")
                )
                .setGuildOnly(false),
            Commands.slash("clear", "🧹 Clear messages in the channel (Admins only)")
                .addOption(OptionType.INTEGER, "amount", "Number of messages to delete (1-100)", true)
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)),
            Commands.slash("suggest", "💡 Submit your suggestion or idea!")
                .setGuildOnly(true),
            Commands.slash("bugreport", "🐞 Report a bug or issue you encountered!")
                .setGuildOnly(true),
            Commands.slash("dependencies", "🔍 Search for Maven, NPM, or Yarn dependencies.")
                .addOption(OptionType.STRING, "package", "📦 Name of the package to search for.", true, true)
                .setGuildOnly(false),
            Commands.slash("snapcode", "📸 Generate a stylish code snapshot.")
                .setDescription("💾 Paste your code and get a beautifully formatted image snapshot! 🎨")
                .setGuildOnly(false)
        ).queue()
    }
    build()
}
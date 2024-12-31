@file:Suppress("SpellCheckingInspection")

import apps.UserInfoApp
import commands.admin.*
import commands.general.*
import commands.utility.Dependencies
import commands.utility.SnapCode
import events.FactOfTheDay
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
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import utils.MongoDBManager
import utils.NekoCLIApi

fun main() {
    val api = NekoCLIApi()
    api.createResourcesFolder()
    val mongoManager = MongoDBManager()
    mongoManager.connect()

    AnsiConsole.systemInstall()

    val jdaBuilder = JDABuilder.createDefault(api.getEnv("TOKEN"))
        .disableCache(
            CacheFlag.MEMBER_OVERRIDES,
            CacheFlag.ACTIVITY,
            CacheFlag.EMOJI,
            CacheFlag.STICKER,
            CacheFlag.SCHEDULED_EVENTS,
            CacheFlag.VOICE_STATE
        )
        .setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER))
        .setChunkingFilter(ChunkingFilter.NONE)
        .enableIntents(
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.MESSAGE_CONTENT,
            *GatewayIntent.entries.toTypedArray()
        )
        .setLargeThreshold(100)
        .setBulkDeleteSplittingEnabled(false)

    println(Ansi.ansi().fgBrightBlue().a("[Info]").reset().a(" Building JDA instance...").reset())
    val jda = jdaBuilder.build().awaitReady()
    println(Ansi.ansi().fgBrightBlue().a("[Info]").reset().a(" JDA instance built and ready.").reset())

    api.initializeJda(jda)
    println(Ansi.ansi().fgBrightBlue().a("[Info]").reset().a(" Adding listeners...").reset())

    jda.addEventListener(
        Ready(),
        WorkerErrors(),
        ModalAskHR(),
        WelcomeAndBye(),
        Ban(),
        StopBot(),
        Pex(),
        Depex(),
        Announce(),
        Help(),
        UserInfo(),
        UserInfoApp(),
        SetAskToHRModal(),
        Sponsors(),
        VerificationSystem(),
        TicketForums(api),
        Status(),
        PassGen(),
        Clear(),
        Suggest(),
        BugReport(),
        Dependencies(),
        SnapCode(),
        FactOfTheDay(api),
        Warn(mongoManager),
        Kick(),
        TimeOut(),
        TempBan(mongoManager, api)
    )
    println(Ansi.ansi().fgBrightBlue().a("[Info]").reset().a(" Listeners added successfully.").reset())

    println(Ansi.ansi().fgBrightBlue().a("[Info]").reset().a(" Registering slash commands...").reset())
    jda.updateCommands().addCommands(
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
                        SubcommandData("list", "📋 View all your amazing sponsors!")
                            .setDescription("📋 List and explore the details of all your sponsors."),
                        SubcommandData("add", "➕ Add a sponsor to the list!")
                            .setDescription("➕ Add a new sponsor with all the details you want!")
                            .addOption(OptionType.STRING, "name", "🏷️ Name of the sponsor", true)
                            .addOption(OptionType.STRING, "imagelink", "🖼️ Image URL for the sponsor logo", true)
                            .addOption(OptionType.STRING, "link", "🔗 Website URL of the sponsor", true)
                            .addOption(OptionType.STRING, "color", "🎨 Hexadecimal color code (e.g., #FFFFFF)", false)
                            .addOption(OptionType.BOOLEAN, "icononly", "🖼️ Show only the icon?", false)
                            .addOption(OptionType.INTEGER, "iconsize", "📏 Size of the icon (in pixels)", false),
                        SubcommandData("remove", "➖ Remove a sponsor!")
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
            .setGuildOnly(false),
        Commands.slash("bugreport", "🐞 Report a bug or issue you encountered!")
            .setGuildOnly(false),
        Commands.slash("dependencies", "🔍 Search for Maven, NPM, or Yarn dependencies.")
            .addOption(OptionType.STRING, "package", "📦 Name of the package to search for.", true, true)
            .setGuildOnly(false),
        Commands.slash("snapcode", "📸 Generate a stylish code snapshot.")
            .setDescription("💾 Paste your code and get a beautifully formatted image snapshot! 🎨")
            .setGuildOnly(false),
        Commands.slash("tempban", "⏳ Manage temporary bans on the server.")
            .setGuildOnly(true)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
            .addSubcommands(
                SubcommandData("add", "➕ Temporarily ban a user.")
                    .addOption(OptionType.USER, "user", "👤 The user to ban.", true)
                    .addOption(OptionType.STRING, "duration", "⏰ Duration of the ban (e.g., 1h, 30m, 1d).", true)
                    .addOption(OptionType.STRING, "reason", "✍️ Reason for the ban.", false),
                SubcommandData("list", "📋 List all active temporary bans."),
                SubcommandData("remove", "❌ Remove a temporary ban by user ID.")
                    .addOption(OptionType.STRING, "user", "🆔 The ID of the user to unban.", true)
            ),
        Commands.slash("kick", "👢 Kick a user from the server.")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS))
            .setGuildOnly(true)
            .addOption(OptionType.USER, "user", "👤 The user to kick.", true)
            .addOption(OptionType.STRING, "reason", "📝 Reason for the kick.", false),
        Commands.slash("timeout", "⏳ Temporarily timeout a user.")
            .setGuildOnly(true)
            .addOptions(
                OptionData(OptionType.USER, "user", "👤 The user to timeout.", true),
                OptionData(OptionType.STRING, "duration", "⏰ Duration of the timeout (e.g., 10m, 2h, 1d).", true),
                OptionData(OptionType.STRING, "reason", "📝 Reason for the timeout.", false)
            ),
        Commands.slash("warn", "⚠️ Manage user warnings.")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES))
            .setGuildOnly(true)
            .addSubcommands(
                SubcommandData("add", "🔔 Add a warning to a user.")
                    .addOption(OptionType.USER, "user", "👤 The user to warn.", true)
                    .addOption(OptionType.STRING, "reason", "✍️ Reason for the warning.", false),
                SubcommandData("info", "🔍 View warnings of a user.")
                    .addOption(OptionType.USER, "user", "👤 The user to check warnings for.", true),
                SubcommandData("remove", "🗑️ Remove the most recent warning from a user.")
                    .addOption(OptionType.USER, "user", "👤 The user to remove a warning from.", true)
            ),
        Commands.user("User Info")
            .setGuildOnly(true)
        ).queue()
    println(Ansi.ansi().fgBrightBlue().a("[Info]").reset().a(" Slash commands registered successfully.").reset())
}
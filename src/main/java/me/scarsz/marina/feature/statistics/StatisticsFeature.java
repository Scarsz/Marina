package me.scarsz.marina.feature.statistics;

import lombok.Getter;
import me.scarsz.marina.Command;
import me.scarsz.marina.Marina;
import me.scarsz.marina.feature.AbstractFeature;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jongo.MongoCollection;

public class StatisticsFeature extends AbstractFeature {

    @Getter private final MongoCollection statisticsCollection;

    public StatisticsFeature() {
        super();
        this.statisticsCollection = Marina.getInstance().getDatastore().getCollection("statistics");

        getJda().upsertCommand("statistics", "Manage statistics channels")
                .addSubcommandGroups(new SubcommandGroupData("create", "Create a new statistics channel")
                        .addSubcommands(new SubcommandData("members", "Create a new members count statistics channel"))
                        .addSubcommands(new SubcommandData("bservers", "Create a new bStats server count statistics channel")
                                .addOption(OptionType.INTEGER, "pluginid", "bStats plugin ID number", true)
                        )
                        .addSubcommands(new SubcommandData("channels", "Create a new category channel count statistics channel")
                                .addOption(OptionType.CHANNEL, "channel", "Child channel in target channel category", true)
                                .addOption(OptionType.STRING, "prefix", "Few character prefix describing the statistics channel (ex: `\uD83C\uDFAB Tickets`)", true)
                                .addOption(OptionType.INTEGER, "delta", "Additional number to sum into channel count")
                        )
                        .addSubcommands(new SubcommandData("ghlatest", "Create a new latest version statistics channel")
                                .addOption(OptionType.STRING, "repository", "Repository to pull release from (ex: `Scarsz/Marina`)", true)
                        )
                        .addSubcommands(new SubcommandData("spigotlatest", "Create a new latest version statistics channel")
                                .addOption(OptionType.INTEGER, "pluginid", "Spigot plugin ID number", true)
                        )
                )
                .queue();
    }

    @Command(name = "members")
    public void createMembersStatisticsChannelCommand(SlashCommandEvent event) {
        String tagName = event.getOption("tag").getAsString();
        StatisticChannel tag = StatisticChannel.findByName(tagName);
        User user = event.getOption("user").getAsUser();

        if (tag != null) {

        } else {
            event.getHook().editOriginal("❌ A tag doesn't exist by the name `" + tagName + "`").queue();
        }
    }

}

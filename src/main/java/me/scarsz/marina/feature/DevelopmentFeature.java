package me.scarsz.marina.feature;

import me.scarsz.marina.Command;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class DevelopmentFeature extends AbstractFeature {

    public DevelopmentFeature() {
        super();

        getJda().upsertCommand("dev", "Developer commands")
                .addSubcommands(new SubcommandData("rejoin", "Make the bot leave the server to be re-invited"))
                .queue();
    }

    @Command(name = "dev.rejoin", permission = "dev")
    public void rejoinCommand(SlashCommandInteractionEvent event) throws IllegalArgumentException {
        event.getHook()
                .setEphemeral(true)
                .editOriginal("https://discord.com/oauth2/authorize?scope=bot+applications.commands&client_id=" + getJda().getSelfUser().getId())
                .queue(v -> event.getGuild().leave().queue());
    }

}

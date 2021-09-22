package me.scarsz.marina.feature.tags;

import lombok.Getter;
import me.scarsz.marina.Command;
import me.scarsz.marina.Marina;
import me.scarsz.marina.feature.AbstractFeature;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jongo.MongoCollection;

public class TagsFeature extends AbstractFeature {

    @Getter private final MongoCollection tagsCollection;

    public TagsFeature() {
        super();
        this.tagsCollection = Marina.getInstance().getDatastore().getCollection("tags");

        getJda().upsertCommand("tag", "Manage message tags")
                .addOption(OptionType.STRING, "tag", "Tag name", true)
                .addOption(OptionType.USER, "user", "Mention user", false)

                .addSubcommands(new SubcommandData("list", "List available tags")
                        .addOption(OptionType.STRING, "search", "Text to filter tags by")
                )
                .addSubcommands(new SubcommandData("edit", "Edit (or create a new) tag")
                        .addOption(OptionType.STRING, "tag", "Tag name to edit", true)
                )
                .addSubcommands(new SubcommandData("delete", "Delete a tag")
                        .addOption(OptionType.STRING, "tag", "Tag name to delete", true)
                )
                .queue();
    }

    @Command("tag")
    public void tagCommand(SlashCommandEvent event) {
        String tagName = event.getOption("tag").getAsString();
        Tag tag = Tag.findByName(tagName);
        User user = event.getOption("user").getAsUser();

        if (tag != null) {

        } else {
            event.getHook().editOriginal("❌ A tag doesn't exist by the name `" + tagName + "`").queue();
        }
    }
    @Command("list")
    public void listTagsCommand(SlashCommandEvent event) {
        String search = event.getOption("search").getAsString();
    }
    @Command(value = "edit", permission = "tags")
    public void editTagCommand(SlashCommandEvent event) {
        String tagName = event.getOption("tag").getAsString();
        Tag tag = Tag.findByName(tagName);

        if (tag != null) {

        } else {
            event.getHook().editOriginal("❌ A tag doesn't exist by the name `" + tagName + "`").queue();
        }
    }
    @Command(value = "delete", permission = "tags")
    public void deleteTagCommand(SlashCommandEvent event) {
        String tagName = event.getOption("tag").getAsString();
        Tag tag = Tag.findByName(tagName);

        if (tag != null) {

        } else {
            event.getHook().editOriginal("❌ A tag doesn't exist by the name `" + tagName + "`").queue();
        }
    }

}

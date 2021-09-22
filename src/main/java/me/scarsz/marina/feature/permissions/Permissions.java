package me.scarsz.marina.feature.permissions;

import com.mongodb.MongoException;
import lombok.Getter;
import me.scarsz.marina.Command;
import me.scarsz.marina.Marina;
import me.scarsz.marina.exception.InsufficientPermissionException;
import me.scarsz.marina.feature.AbstractFeature;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jongo.MongoCollection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Permissions extends AbstractFeature {

    private static final Set<String> SUPERUSERS = new HashSet<>(Arrays.asList(System.getenv("SUPERUSERS").split(",")));
    @Getter private final MongoCollection snowflakesCollection;

    public Permissions() {
        super();

        MongoCollection usersCollection = Marina.getInstance().getDatastore().getCollection("users");
        if (usersCollection.count() > 0) {
            try {
                usersCollection.getDBCollection().rename("snowflakes");
            } catch (MongoException ignored) {
                // we tried
            }
        }
        this.snowflakesCollection = Marina.getInstance().getDatastore().getCollection("snowflakes");

        getJda().upsertCommand("permission", "Manage permissions")
                .addSubcommands(new SubcommandData("grant", "Grant a permission to a user")
                        .addOption(OptionType.USER, "user", "Target user", true)
                        .addOption(OptionType.STRING, "permission", "Permission to grant", true)
                )
                .addSubcommands(new SubcommandData("remove", "Remove a permission from a user")
                        .addOption(OptionType.USER, "user", "Target user", true)
                        .addOption(OptionType.STRING, "permission", "Permission to remove", true)
                )
                .addSubcommands(new SubcommandData("list", "List granted permissions for the specified user")
                        .addOption(OptionType.USER, "user", "Target user", true)
                )
                .addSubcommands(new SubcommandData("test", "Test a permission check on a user")
                        .addOption(OptionType.USER, "user", "Target user", true)
                        .addOption(OptionType.STRING, "permission", "Permission to check", true)
                )
                .queue();
    }

    @Command(name = "grant", permission = "admin")
    public void grantCommand(SlashCommandEvent event) {
        User targetUser = event.getOption("user").getAsUser();
        String permission = event.getOption("permission").getAsString();

        DiscordEntity user = new DiscordEntity();
        user.setId(targetUser.getId());
        user.setPermissions(Collections.singleton(permission));

        DiscordEntity.upsertBySnowflake(targetUser).with(user);

        event.getHook().editOriginal("✅ Added permission `" + permission + "` to " + targetUser.getAsMention()).complete();
    }
    @Command(name = "remove", permission = "admin")
    public void removeCommand(SlashCommandEvent event) {
        User targetUser = event.getOption("user").getAsUser();
        String permission = event.getOption("permission").getAsString();

        Set<String> newPermissions = new HashSet<>(getPermissions(targetUser));
        newPermissions.remove(permission);

        DiscordEntity user = new DiscordEntity();
        user.setId(targetUser.getId());
        user.setPermissions(newPermissions);

        DiscordEntity.upsertBySnowflake(targetUser).with(user);

        event.getHook().editOriginal("✅ Removed permission `" + permission + "` from " + targetUser.getAsMention()).complete();
    }
    @Command(name = "list", permission = "admin")
    public void listCommand(SlashCommandEvent event) {
        User targetUser = event.getOption("user").getAsUser();

        DiscordEntity discordUser = DiscordEntity.findBySnowflake(targetUser);
        if (discordUser != null) {
            event.getHook().editOriginal(String.format(
                    "✅ %s's permissions: ```\n%s\n```",
                    targetUser.getAsMention(),
                    String.join("\n", discordUser.getPermissions())
            )).complete();
        } else {
            event.getHook().editOriginal("❌ " + targetUser.getAsMention() + " has no granted permissions").complete();
        }
    }
    @Command(name = "test", permission = "admin")
    public void testCommand(SlashCommandEvent event) {
        User targetUser = event.getOption("user").getAsUser();
        String permission = event.getOption("permission").getAsString();

        boolean value = hasPermission(targetUser, permission);
        event.getHook().editOriginal(targetUser.getAsMention() + "'s value for `" + permission + "`: " + (value ? "✅" : "❌")).complete();
    }

    public boolean hasPermission(DiscordEntity user, String permission) {
        do {
            if (user.getPermissions().contains(permission)) return true;
        } while (permission.contains(".") && (permission = permission.substring(0, permission.lastIndexOf("."))).contains("."));
        return false;
    }
    public boolean hasPermission(ISnowflake snowflake, String permission) {
        if (SUPERUSERS.contains(snowflake.getId())) return true;
        DiscordEntity user = DiscordEntity.findBySnowflake(snowflake);
        return user != null && hasPermission(user, permission);
    }
    public void checkPermission(ISnowflake snowflake, String permission) throws InsufficientPermissionException {
        if (!hasPermission(snowflake, permission)) {
            throw new InsufficientPermissionException(permission);
        }
    }
    public Set<String> getPermissions(ISnowflake snowflake) {
        DiscordEntity user = DiscordEntity.findBySnowflake(snowflake);
        return user != null ? user.getPermissions() : Collections.emptySet();
    }

}

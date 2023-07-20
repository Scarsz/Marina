package me.scarsz.marina.feature;

import me.scarsz.marina.Command;
import me.scarsz.marina.Marina;
import me.scarsz.marina.exception.InsufficientPermissionException;
import me.scarsz.marina.feature.permissions.Permissions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.StringJoiner;

public abstract class AbstractFeature extends ListenerAdapter implements Feature {

    public AbstractFeature() {
        getJda().addEventListener(this);
        Marina.getInstance().getFeatures().put(this.getClass(), this);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        StringJoiner targetBuilder = new StringJoiner(".");
        targetBuilder.add(event.getName());
        if (event.getSubcommandGroup() != null) targetBuilder.add(event.getSubcommandGroup());
        if (event.getSubcommandName() != null) targetBuilder.add(event.getSubcommandName());
        String target = targetBuilder.toString();

        for (Method method : getClass().getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if (command != null && command.name().equalsIgnoreCase(target)) {
                if (!command.permission().isEmpty() && !Marina.getFeature(Permissions.class).hasPermission(event.getUser(), command.permission())) {
                    event.reply("❌ Insufficient permission: " + command.permission()).queue();
                    return;
                }

                event.deferReply().complete();
                try {
                    method.invoke(this, event);
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof InsufficientPermissionException) {
                        String permission = ((InsufficientPermissionException) e.getCause()).getPermission();
                        event.getHook().editOriginal("❌ Insufficient permission: " + permission).queue();
                    } else if (e.getCause() instanceof IllegalArgumentException) {
                        event.getHook().editOriginal("❌ " + e.getCause().getMessage()).queue();
                    } else {
                        event.getHook().editOriginal("❌ " + e.getCause().getMessage()).queue();
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    event.getHook().editOriginal("❌ " + e.getCause().getMessage()).queue();
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    protected boolean hasPermission(ISnowflake snowflake, String permission) {
        return Marina.getFeature(Permissions.class).hasPermission(snowflake, permission);
    }
    protected void checkPermission(ISnowflake snowflake, String permission) throws InsufficientPermissionException {
        Marina.getFeature(Permissions.class).checkPermission(snowflake, permission);
    }

    protected JDA getJda() {
        return Marina.getInstance().getJda();
    }

}

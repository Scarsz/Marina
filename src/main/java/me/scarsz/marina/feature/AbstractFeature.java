package me.scarsz.marina.feature;

import lombok.SneakyThrows;
import me.scarsz.marina.Command;
import me.scarsz.marina.Marina;
import me.scarsz.marina.exception.InsufficientPermissionException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class AbstractFeature extends ListenerAdapter implements Feature {

    public AbstractFeature() {
        // register feature to receive events from JDA
        getJda().addEventListener(this);
    }

    @Override
    @SneakyThrows
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        String target = event.getSubcommandName() != null ? event.getSubcommandName() : event.getName();

        for (Method method : getClass().getDeclaredMethods()) {
            Command command = method.getAnnotation(Command.class);
            if (command != null && command.value().equalsIgnoreCase(target)) {
                if (!command.permission().isEmpty() && !Marina.getInstance().getPermissions().hasPermission(event.getUser(), command.permission())) {
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
        return Marina.getInstance().getPermissions().hasPermission(snowflake, permission);
    }
    protected void checkPermission(ISnowflake snowflake, String permission) throws InsufficientPermissionException {
        Marina.getInstance().getPermissions().checkPermission(snowflake, permission);
    }

    protected JDA getJda() {
        return Marina.getInstance().getJda();
    }

}

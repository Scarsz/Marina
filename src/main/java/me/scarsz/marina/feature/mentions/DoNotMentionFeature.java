package me.scarsz.marina.feature.mentions;

import me.scarsz.marina.feature.AbstractFeature;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class DoNotMentionFeature extends AbstractFeature {

    private final Map<ISnowflake, Map<ISnowflake, Long>> pingedMembers = new HashMap<>();

    public DoNotMentionFeature() {
        super();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        Role doNotMentionRole = getDoNotMentionRole(event.getGuild());
        if (doNotMentionRole == null) return;

        Message referencedMessage = event.getMessage().getReferencedMessage();
        boolean referencedMessageCheck = referencedMessage != null && referencedMessage.getMember() != null && check(event.getMessage(), referencedMessage.getMember());
        boolean mentionedMembersCheck = event.getMessage().getMentions().getMembers().stream().anyMatch(member -> check(event.getMessage(), member));
        if (referencedMessageCheck || mentionedMembersCheck) {
            event.getMessage().reply("Hey " + event.getMember().getAsMention() + "! Please don't ping people in this server that have the " + doNotMentionRole.getAsMention() + " role. Thank you.")
                    .setAllowedMentions(EnumSet.of(Message.MentionType.USER))
                    .queue();
        }
    }

    private boolean check(Message message, Member member) {
        Map<ISnowflake, Long> pinged = pingedMembers.computeIfAbsent(message.getAuthor(), s -> new HashMap<>());
        if (member.equals(message.getMember())) return false; // people are allowed to ping themselves for whatever reason

        pinged.put(member, System.currentTimeMillis());
        Long lastPinged = pingedMembers.getOrDefault(member, new HashMap<>()).get(message.getAuthor());

        boolean hasBeenPingedByMemberRecently = lastPinged != null && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - lastPinged) < 15;
        boolean hasPermissionToPingMember = hasPermission(message.getAuthor(), "mention." + member.getId());
        boolean isMessageModerator = message.getMember().hasPermission(Permission.MESSAGE_MANAGE);
        boolean isSayingThankYou = System.getenv("ALLOW_MENTIONS_IN_THANKS").equalsIgnoreCase("true")
                && (message.getContentRaw().toLowerCase(Locale.ROOT).contains("thank you")
                    || Arrays.stream(message.getContentRaw().split(" ")).anyMatch(s -> s.equalsIgnoreCase("ty")));

        return !hasBeenPingedByMemberRecently
                && !hasPermissionToPingMember
                && !isMessageModerator
                && !isSayingThankYou
                && member.getRoles().contains(getDoNotMentionRole(message.getGuild()));
    }

    public @Nullable Role getDoNotMentionRole(Guild guild) {
        String roleRaw = System.getenv("DO_NOT_MENTION_ROLE");
        if (StringUtils.isNotBlank(roleRaw)) {
            if (StringUtils.isNumeric(roleRaw)) {
                return guild.getRoleById(roleRaw);
            } else {
                return guild.getRolesByName(roleRaw, true).stream().findFirst().orElse(null);
            }
        } else {
            return null;
        }
    }

}

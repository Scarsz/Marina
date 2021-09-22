package me.scarsz.marina.feature.statistics.handler;

import me.scarsz.marina.Marina;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;

public class MemberCountStatistic implements StatisticProvider {

    @Override
    public String compute(Map<String, Object> data) {
        String guildId = (String) data.get("guild");
        Guild guild = Marina.getInstance().getJda().getGuildById(guildId);
        if (guild != null) {
            return String.valueOf(guild.getMemberCount());
        } else {
            return "0";
        }
    }

    @Override
    public String format(Map<String, Object> data) {
        return "\uD83E\uDDCD Members: " + compute(data);
    }

}

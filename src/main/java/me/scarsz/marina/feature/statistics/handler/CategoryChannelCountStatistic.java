package me.scarsz.marina.feature.statistics.handler;

import me.scarsz.marina.Marina;
import net.dv8tion.jda.api.entities.Category;

import java.util.Map;

public class CategoryChannelCountStatistic implements StatisticProvider {

    @Override
    public Integer compute(Map<String, Object> data) {
        Category category = Marina.getInstance().getJda().getCategoryById(String.valueOf(data.get("category")));
        if (category == null) return null;
        return category.getChannels().size() + (int) data.getOrDefault("delta", 0);
    }

    @Override
    public String format(Map<String, Object> data) {
        return data.get("prefix") + ": " + compute(data);
    }

}

package me.scarsz.marina.feature.statistics.handler;

import java.util.Map;

public interface StatisticProvider {

    String compute(Map<String, Object> data);
    String format(Map<String, Object> data);

}

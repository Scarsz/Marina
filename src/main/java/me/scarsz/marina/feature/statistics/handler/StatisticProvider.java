package me.scarsz.marina.feature.statistics.handler;

import java.util.Map;

public interface StatisticProvider {

    Object compute(Map<String, Object> data) throws Exception;
    String format(Map<String, Object> data) throws Exception;

}

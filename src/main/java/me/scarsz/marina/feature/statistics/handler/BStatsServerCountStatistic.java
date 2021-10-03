package me.scarsz.marina.feature.statistics.handler;

import alexh.weak.Dynamic;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;

public class BStatsServerCountStatistic implements StatisticProvider {

    @Override
    public String compute(Map<String, Object> data) throws ParseException {
        HttpRequest request = HttpRequest.get("https://bstats.org/api/v1/plugins/" + data.get("pluginId") + "/charts/pluginVersion/data").acceptJson();
        Dynamic dynamic = Dynamic.from(new JSONParser().parse(request.body()));
        return null;
    }

    @Override
    public String format(Map<String, Object> data) {
        return null;
    }

}

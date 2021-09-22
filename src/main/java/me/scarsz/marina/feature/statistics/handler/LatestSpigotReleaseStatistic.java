package me.scarsz.marina.feature.statistics.handler;

import alexh.weak.Dynamic;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;

public class LatestSpigotReleaseStatistic implements StatisticProvider {

    @Override
    public String compute(Map<String, Object> data) throws ParseException {
        HttpRequest request = HttpRequest.get("https://api.spiget.org/v2/resources/" + data.get("pluginId") + "/updates/latest").acceptJson();

        if (request.code() != 200) return null;
        Dynamic title = Dynamic.from(new JSONParser().parse(request.body())).get("title");
        return title.isPresent() ? title.asString() : null;
    }

    @Override
    public String format(Map<String, Object> data) throws ParseException {
        return "\uD83D\uDD16 Release: " + compute(data);
    }

}

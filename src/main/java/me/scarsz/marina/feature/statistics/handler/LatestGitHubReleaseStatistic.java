package me.scarsz.marina.feature.statistics.handler;

import alexh.weak.Dynamic;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;

public class LatestGitHubReleaseStatistic implements StatisticProvider {

    @Override
    public String compute(Map<String, Object> data) throws ParseException {
        HttpRequest request = HttpRequest.get("https://api.github.com/repos/DiscordSRV/DiscordSRV/releases/latest");
        if (request.code() != 200) return null;
        Dynamic tagName = Dynamic.from(new JSONParser().parse(request.body())).get("tag_name");
        return tagName.isPresent() ? tagName.asString() : null;
    }

    @Override
    public String format(Map<String, Object> data) throws ParseException {
        return "\uD83D\uDD16 Release: " + compute(data);
    }

}

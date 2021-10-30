package me.scarsz.marina.feature.http;

import io.javalin.Javalin;
import lombok.Getter;
import me.scarsz.marina.feature.AbstractFeature;
import org.apache.commons.lang.StringUtils;

public class HttpFeature extends AbstractFeature {

    @Getter private final String baseUrl = StringUtils.isNotBlank(System.getenv("HTTP_URL"))
            ? System.getenv("HTTP_URL")
            : "https://marina.scarsz.me";
    @Getter private final Javalin app;

    public HttpFeature() {
        this.app = Javalin.create(config -> {
                    config.showJavalinBanner = false;
                })
                .get("/", ctx -> ctx.redirect("https://github.com/Scarsz/Marina"))
                .start("0.0.0.0", 34117);
    }

}

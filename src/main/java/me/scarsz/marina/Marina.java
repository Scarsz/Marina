package me.scarsz.marina;

import com.mongodb.MongoClient;
import lombok.Getter;
import me.scarsz.jdaappender.ChannelLoggingHandler;
import me.scarsz.marina.feature.AbstractFeature;
import me.scarsz.marina.feature.DevelopmentFeature;
import me.scarsz.marina.feature.docker.DockerFeature;
import me.scarsz.marina.feature.http.HttpFeature;
import me.scarsz.marina.feature.mentions.DoNotMentionFeature;
import me.scarsz.marina.feature.paste.PasteFeature;
import me.scarsz.marina.feature.permissions.Permissions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.commons.lang.StringUtils;
import org.jongo.Jongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class Marina {

    @Getter private static Marina instance;
    @Getter private static final Logger logger = LoggerFactory.getLogger(Marina.class);

    @Getter private final Jongo datastore;
    @Getter private final JDA jda;
    @Getter private final Map<Class<? extends AbstractFeature>, AbstractFeature> features = new HashMap<>();

    public Marina() throws LoginException, InterruptedException {
        Marina.instance = this;

        this.datastore = new Jongo(new MongoClient("mongo:27017").getDB("marina"));

        this.jda = JDABuilder.create(System.getenv("TOKEN"), EnumSet.complementOf(EnumSet.of(
                GatewayIntent.GUILD_MESSAGE_TYPING,
                GatewayIntent.DIRECT_MESSAGE_TYPING,
                GatewayIntent.GUILD_PRESENCES
                )))
                .setActivity(
                        StringUtils.isNotBlank(System.getenv("WATCHING")) ?
                                Activity.watching(System.getenv("WATCHING"))
                        : StringUtils.isNotBlank(System.getenv("PLAYING")) ?
                                Activity.playing(System.getenv("PLAYING"))
                        : Activity.watching("🚢 ⛵")
                )
                .build();
        if (StringUtils.isNotBlank(System.getenv("LOGGING_CHANNEL"))) {
            new ChannelLoggingHandler(() -> this.jda.getTextChannelById(System.getenv("LOGGING_CHANNEL")), c -> {
                c.mapLoggerName("com.github.dockerjava", "Docker");
                c.mapLoggerName("net.dv8tion.jda", "JDA");
                c.mapLoggerName("org.mongodb", "Mongo");
                c.mapLoggerName("io.javalin", "HTTP");
                c.mapLoggerName("org.eclipse.jetty", "Jetty");
                c.mapLoggerNameFriendly("me.scarsz.marina.feature", name -> name.replace("Feature", " Feature"));
                c.mapLoggerNameFriendly("me.scarsz.marina", name -> "Marina > " + name);
                c.addFilter(logItem -> logItem.getMessage().contains("dockerHttpClient")); // useless warning
            }).attachJavaLogging().schedule();
        }
        this.jda.awaitReady();

        // features
        new Permissions();
        new DevelopmentFeature();
        new HttpFeature();
        new DockerFeature();
//        try { new PasteFeature(); } catch (IOException e) { e.printStackTrace(); }
        new DoNotMentionFeature();
//        new TagsFeature();
//        new StatisticsFeature();
    }

    public static <F extends AbstractFeature> F getFeature(Class<F> clazz) {
        //noinspection unchecked
        return (F) Marina.instance.getFeatures().get(clazz);
    }

}

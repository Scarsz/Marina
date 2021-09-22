package me.scarsz.marina.feature.statistics;

import lombok.Data;
import me.scarsz.marina.Marina;
import me.scarsz.marina.feature.tags.TagsFeature;
import org.jongo.MongoCollection;
import org.jongo.Update;
import org.jongo.marshall.jackson.oid.MongoId;

import java.awt.*;
import java.util.Set;

@Data
public class StatisticChannel {

    public static StatisticChannel findByName(String name) {
        return getCollection().findOne("{name: #}", name).as(StatisticChannel.class);
    }
    public static Update upsertByName(String name) {
        return getCollection().update("{name: #}", name).upsert();
    }
    public static MongoCollection getCollection() {
        return Marina.getFeature(TagsFeature.class).getTagsCollection();
    }

    @MongoId String name;
    Set<String> aliases;
    Color color;
    String title;
    String thumbnailUrl;
    String content;

}

package me.scarsz.marina.feature.tags;

import lombok.Data;
import me.scarsz.marina.Marina;
import org.jongo.Update;
import org.jongo.marshall.jackson.oid.MongoId;

@Data
public class Tag {

    public static Tag findByName(String name) {
        return Marina.getFeature(TagsFeature.class).getTagsCollection().findOne("{name: #}", name).as(Tag.class);
    }
    public static Update upsertByName(String name) {
        return Marina.getInstance().getUsersCollection().update("{name: #}", name).upsert();
    }

    @MongoId String name;

}

package me.scarsz.marina.feature.permissions;

import lombok.Data;
import me.scarsz.marina.Marina;
import net.dv8tion.jda.api.entities.ISnowflake;
import org.jongo.MongoCollection;
import org.jongo.Update;
import org.jongo.marshall.jackson.oid.MongoId;

import java.util.Set;

@Data
public class DiscordEntity {

    public static DiscordEntity findBySnowflake(ISnowflake snowflake) {
        return getCollection().findOne("{id: #}", snowflake.getId()).as(DiscordEntity.class);
    }
    public static Update upsertBySnowflake(ISnowflake snowflake) {
        return getCollection().update("{id: #}", snowflake.getId()).upsert();
    }
    public static MongoCollection getCollection() {
        return Marina.getFeature(Permissions.class).getSnowflakesCollection();
    }

    @MongoId String id;
    Set<String> permissions;

}

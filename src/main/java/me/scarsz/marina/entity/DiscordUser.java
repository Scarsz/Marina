package me.scarsz.marina.entity;

import lombok.Data;
import me.scarsz.marina.Marina;
import net.dv8tion.jda.api.entities.ISnowflake;
import org.jongo.Update;
import org.jongo.marshall.jackson.oid.MongoId;

import java.util.Set;

@Data
public class DiscordUser {

    public static DiscordUser findBySnowflake(ISnowflake snowflake) {
        return Marina.getInstance().getUsersCollection().findOne("{id: #}", snowflake.getId()).as(DiscordUser.class);
    }
    public static Update upsertBySnowflake(ISnowflake snowflake) {
        return Marina.getInstance().getUsersCollection().update("{id: #}", snowflake.getId()).upsert();
    }

    @MongoId String id;
    Set<String> permissions;

}

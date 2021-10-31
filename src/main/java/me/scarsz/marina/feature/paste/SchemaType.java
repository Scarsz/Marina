package me.scarsz.marina.feature.paste;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public enum SchemaType {

    JSON,
    YAML;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Yaml SNAKEYAML = new Yaml(new DumperOptions() {{
        setPrettyFlow(true);
    }});

    public Object parse(String data) throws JsonSyntaxException, YAMLException {
        if (this == JSON) {
            return JsonParser.parseString(data);
        } else if (this == YAML) {
            return SNAKEYAML.load(data);
        } else {
            throw new RuntimeException("Invalid SchemaType");
        }
    }

    public String dump(Object data) {
        if (this == JSON) {
            return GSON.toJson(data);
        } else if (this == YAML) {
            return SNAKEYAML.dumpAsMap(data);
        } else {
            throw new RuntimeException("Invalid SchemaType");
        }
    }

}

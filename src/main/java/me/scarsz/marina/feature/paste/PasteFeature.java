package me.scarsz.marina.feature.paste;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import lombok.SneakyThrows;
import me.scarsz.marina.Marina;
import me.scarsz.marina.feature.AbstractFeature;
import me.scarsz.marina.feature.http.HttpFeature;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class PasteFeature extends AbstractFeature {

    private final File fileContainer = new File("pastes");
    private final Yaml yaml = new Yaml(new DumperOptions() {{
        setIndent(4);
        setPrettyFlow(true);
    }});
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PasteFeature() throws IOException {
        super();

        if (!fileContainer.exists()) {
            if (!fileContainer.mkdir()) {
                throw new IOException("Failed to create pastes file directory " + fileContainer.getPath());
            }
        }

        Marina.getFeature(HttpFeature.class).getApp().routes(() -> {
            path("paste/{guild}/{channel}/{message}/{index}", () -> {
                get(ctx -> viewPaste(ctx, false));
                get("lines", ctx -> viewPaste(ctx, true));
                get("parsed", this::viewPasteParsed);
            });
        });
    }

    private void viewPaste(Context ctx, boolean lines) throws IOException {
        String guild = ctx.pathParam("guild");
        String channel = ctx.pathParam("guild");
        String message = ctx.pathParam("guild");
        String index = ctx.pathParam("index");
        File file = getFile(guild, channel, message, index);

        StringJoiner joiner = new StringJoiner("\n");
        try (FileReader fileReader = new FileReader(file)) {
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                int lineCounter = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    joiner.add((lines ? "[" + ++lineCounter + "] " : "") + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ctx.result(joiner.toString());
    }
    private void viewPasteParsed(Context ctx) throws IOException {
        String guild = ctx.pathParam("guild");
        String channel = ctx.pathParam("guild");
        String message = ctx.pathParam("guild");
        String index = ctx.pathParam("index");
        File file = getFile(guild, channel, message, index);

        String result;
        switch (FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.ROOT)) {
            case "yml":
            case "yaml":
                try (FileReader reader = new FileReader(file)) {
                    result = yaml.dumpAsMap(yaml.load(reader));
                } catch (ParserException e) {
                    StringJoiner joiner = new StringJoiner("\n");
                    joiner.add("Parsing exception: " + e.getMessage());
                    joiner.add("");
                    joiner.add("Problem: " + e.getProblem());
                    joiner.add("Context: " + e.getContext());
                    result = joiner.toString();
                } catch (FileNotFoundException e) {
                    throw new NotFoundResponse("Paste file not found");
                }
                break;
            case "json":
                try (FileReader reader = new FileReader(file)) {
                    result = gson.toJson(JsonParser.parseReader(reader));
                } catch (JsonSyntaxException e) {
                    result = "Parsing exception: " + e;
                } catch (FileNotFoundException e) {
                    throw new NotFoundResponse("Paste file not found");
                }
                break;
            default:
                throw new BadRequestResponse("Requested file is not parseable");
        }

        ctx.result(result);
    }

    private void processAttachment(Message message, Message.Attachment attachment, int index) throws IOException {
        File file = create(message.getGuild().getId(), message.getChannel().getId(), message.getId(), String.valueOf(index));
        attachment.downloadToFile(file)
                .thenAccept(f -> {
                    String baseUrl = Marina.getFeature(HttpFeature.class).getBaseUrl() + "/paste/" + message.getGuild().getId() + "/" + message.getChannel().getId() + "/" + message.getId() + "-" + index;
                    message.reply("Paste version of `" + attachment.getFileName() + "` from " + message.getAuthor().getAsMention())
                            .setActionRow(
                                    Button.link(baseUrl, "View file"),
                                    Button.link(baseUrl + "/lines", "With line numbers"),
                                    Button.link(baseUrl + "/parsed", "Parsed")
                            )
                            .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                            .mentionRepliedUser(false)
                            .queue();
                })
                .orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        int i = 0;
        for (Message.Attachment attachment : event.getMessage().getAttachments()) {
            processAttachment(event.getMessage(), attachment, ++i);
        }
    }

    @Override
    public void onGuildMessageDelete(@NotNull GuildMessageDeleteEvent event) {
        event.getChannel().getHistoryAfter(event.getMessageId(), 15).queue(history -> {
            for (Message message : history.getRetrievedHistory()) {
                if (!message.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) continue;
                if (message.getActionRows().size() != 1) continue;
                if (!message.getActionRows().get(0).getComponents().stream().allMatch(component -> {
                    if (component instanceof Button button) {
                        if (button.getUrl() == null) return false;
                        return button.getUrl().startsWith(
                                Marina.getFeature(HttpFeature.class).getBaseUrl() + "/paste/"
                                        + event.getGuild().getId() + "/" + event.getChannel().getId() + "/" + event.getMessageId()
                        );
                    }
                    return false;
                })) continue;

                message.delete().queue();
            }
        });

        for (File file : getFiles(event.getGuild().getId(), event.getChannel().getId(), event.getMessageId())) {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    private Collection<File> getFiles(String guild, String channel, String message) {
        File fileFolder = new File(fileContainer, guild + "/" + channel);
        if (fileFolder.exists()) {
            return FileUtils.listFiles(
                    fileFolder,
                    new WildcardFileFilter(message + "-*"), null
            );
        } else {
            return Collections.emptySet();
        }
    }
    private File getFile(String guild, String channel, String message, String index) throws IOException {
        File fileFolder = new File(fileContainer, guild + "/" + channel);
        if (!fileFolder.exists() && !fileFolder.mkdirs()) throw new IOException("Failed to create directory " + fileFolder.getName());
        return FileUtils.listFiles(
                fileFolder,
                new WildcardFileFilter(message + "-" + index + ".*"), null
        ).stream().findFirst().orElse(null);
    }
    private File create(String guild, String channel, String message, String index) throws IOException {
        File fileFolder = new File(fileContainer, guild + "/" + channel);
        if (!fileFolder.exists() && !fileFolder.mkdirs()) throw new IOException("Failed to create directory " + fileFolder.getName());
        return new File(fileFolder, message + "-" + index + ".*");
    }

}

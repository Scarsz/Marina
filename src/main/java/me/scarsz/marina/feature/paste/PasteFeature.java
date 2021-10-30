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
import net.dv8tion.jda.api.interactions.components.Component;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class PasteFeature extends AbstractFeature {

    private final File fileContainer = new File("pastes");
    private final Yaml yaml = new Yaml(new DumperOptions() {{
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
            path("paste/{channel}/{message}", () -> {
                get("raw/{file}", ctx -> viewPaste(ctx, false));
                get("lines/{file}", ctx -> viewPaste(ctx, true));
                get("parsed/{file}", this::viewPasteParsed);
            });
        });
    }

    private void viewPaste(Context ctx, boolean lines) {
        String channel = ctx.pathParam("channel");
        String message = ctx.pathParam("message");
        String fileName = ctx.pathParam("file");
        File file = getFile(channel, message, fileName);
        if (!file.exists()) throw new NotFoundResponse();

        StringJoiner joiner = new StringJoiner("\n");
        try (FileReader fileReader = new FileReader(file)) {
            try (BufferedReader reader = new BufferedReader(fileReader)) {
                String line;
                int lineCounter = 0;
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
        String channel = ctx.pathParam("channel");
        String message = ctx.pathParam("message");
        String fileName = ctx.pathParam("file");
        File file = getFile(channel, message, fileName);
        if (!file.exists()) throw new NotFoundResponse();

        String result;
        switch (FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.ROOT)) {
            case "yml":
            case "yaml":
                try (FileReader reader = new FileReader(file)) {
                    result = yaml.dumpAsMap(yaml.load(reader));
                } catch (YAMLException e) {
                    StringJoiner joiner = new StringJoiner("\n");
                    joiner.add("Parsing exception: " + e.getMessage());
                    if (e instanceof MarkedYAMLException e2) {
                        joiner.add("");
                        joiner.add("Problem: " + e2.getProblem());
                        joiner.add("Context: " + e2.getContext());
                    }
                    result = joiner.toString();
                }
                break;
            case "json":
                try (FileReader reader = new FileReader(file)) {
                    result = gson.toJson(JsonParser.parseReader(reader));
                } catch (JsonSyntaxException e) {
                    result = "Parsing exception: " + e;
                }
                break;
            default:
                throw new BadRequestResponse("Requested file is not parseable");
        }

        ctx.result(result);
    }

    private void processAttachment(Message message, Message.Attachment attachment) {
        File file = getFile(message.getChannel().getId(), message.getId(), attachment.getFileName());
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.ROOT);
        boolean parseable = extension.equals("yml") || extension.equals("yaml") || extension.equals("json");

        String baseUrl = Marina.getFeature(HttpFeature.class).getBaseUrl() + "/paste/" + message.getChannel().getId() + "/" + message.getId();
        List<Component> components = new LinkedList<>();
        components.add(Button.link(baseUrl + "/raw/" + attachment.getFileName(), "View file"));
        components.add(Button.link(baseUrl + "/lines/" + attachment.getFileName(), "With line numbers"));
        if (parseable) components.add(Button.link(baseUrl + "/parsed/" + attachment.getFileName(), "Parsed"));

        attachment.downloadToFile(file)
                .thenAccept(f -> {
                    message.reply("Paste version of `" + attachment.getFileName() + "` from " + message.getAuthor().getAsMention())
                            .setActionRow(components)
                            .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                            .mentionRepliedUser(false)
                            .queue();
                })
                .orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        for (Message.Attachment attachment : event.getMessage().getAttachments()) {
            processAttachment(event.getMessage(), attachment);
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

    private @NotNull File getFile(String channel, String message, String fileName) {
        return new File(fileContainer, channel + "-" + message + "-" + fileName);
    }
    private @NotNull Collection<File> getFiles(String guild, String channel, String message) {
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

}

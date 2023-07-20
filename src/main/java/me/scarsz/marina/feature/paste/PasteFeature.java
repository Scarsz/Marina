package me.scarsz.marina.feature.paste;

import com.google.gson.JsonSyntaxException;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;
import lombok.SneakyThrows;
import me.scarsz.marina.Marina;
import me.scarsz.marina.feature.AbstractFeature;
import me.scarsz.marina.feature.http.HttpFeature;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    private static final String URL_TEMPLATE = Marina.getFeature(HttpFeature.class).getBaseUrl() + "/paste/{type}/{channel}/{message}/{file}";
    private static final File FILE_CONTAINER = new File("pastes");
    private static final List<String> TEXT_FILE_EXTENSIONS = Arrays.asList("txt", "log", "yml", "yaml", "json", "properties");

    public PasteFeature() throws IOException {
        super();

        if (!FILE_CONTAINER.exists()) {
            if (!FILE_CONTAINER.mkdir()) {
                throw new IOException("Failed to create pastes file directory " + FILE_CONTAINER.getPath());
            }
        }

        Marina.getFeature(HttpFeature.class).getApp().routes(() -> {
            path("paste", () -> {
                get("raw/{channel}/{message}/{file}", ctx -> viewPaste(ctx, false));
                get("lines/{channel}/{message}/{file}", ctx -> viewPaste(ctx, true));
                get("parsed/{channel}/{message}/{file}", this::viewPasteParsed);
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
    private void viewPasteParsed(Context ctx) {
        String channel = ctx.pathParam("channel");
        String message = ctx.pathParam("message");
        String fileName = ctx.pathParam("file");
        File file = getFile(channel, message, fileName);
        if (!file.exists()) throw new NotFoundResponse();
        SchemaType schema = getSchemaType(file.getName());
        if (schema == null) throw new BadRequestResponse("Requested file is not parseable");

        String result;
        try {
            result = schema.dump(schema.parse(FileUtils.readFileToString(file, "UTF-8")));
        } catch (JsonSyntaxException e) {
            result = "Parsing exception: " + e;
        } catch (YAMLException e) {
            result = "Parsing exception: " + e.getMessage();
        } catch (IOException e) {
            throw new InternalServerErrorResponse(e.getMessage());
        }
        ctx.result(result);
    }

    @SneakyThrows
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        for (Message.Attachment attachment : event.getMessage().getAttachments()) {
            if (TEXT_FILE_EXTENSIONS.contains(FilenameUtils.getExtension(attachment.getFileName()).toLowerCase(Locale.ROOT))) {
                handleAttachmentPaste(event.getMessage(), attachment);
            }
        }
    }

    private void handleAttachmentPaste(Message message, Message.Attachment attachment) {
        File file = getFile(message.getChannel().getId(), message.getId(), attachment.getFileName());
        SchemaType schema = getSchemaType(file.getName());

        String url = URL_TEMPLATE
                .replace("{channel}", message.getChannel().getId())
                .replace("{message}", message.getId())
                .replace("{file}", attachment.getFileName());

        List<ItemComponent> components = new LinkedList<>();
        components.add(Button.link(url.replace("{type}", "raw"), "View file"));
        components.add(Button.link(url.replace("{type}", "lines"), "With line numbers"));
        if (schema != null) components.add(Button.link(url.replace("{type}", "parsed"), "Parsed"));

        attachment.downloadToFile(file)
                .thenAccept(f -> {
                    if (schema != null) {
                        String parseException = null;
                        try {
                            schema.parse(FileUtils.readFileToString(file, "UTF-8"));
                        } catch (JsonSyntaxException e) {
                            parseException = "Parsing exception: " + e;
                        } catch (YAMLException e) {
                            parseException = "Parsing exception: " + e.getMessage();
                        } catch (IOException e) {
                            throw new InternalServerErrorResponse(e.getMessage());
                        }

                        if (parseException != null) {
                            message.reply("```" + parseException + "\n```").queue();
                        }
                    }

                    message.reply("Paste version of `" + attachment.getFileName() + "` from " + message.getAuthor().getAsMention())
                            .setActionRow(components)
                            .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                            .mentionRepliedUser(false)
                            .queue();
                })
                .orTimeout(5, TimeUnit.SECONDS);
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;

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
        return new File(FILE_CONTAINER, channel + "-" + message + "-" + fileName);
    }
    private @NotNull Collection<File> getFiles(String guild, String channel, String message) {
        File fileFolder = new File(FILE_CONTAINER, guild + "/" + channel);
        if (fileFolder.exists()) {
            return FileUtils.listFiles(
                    fileFolder,
                    new WildcardFileFilter(message + "-*"), null
            );
        } else {
            return Collections.emptySet();
        }
    }

    private static @Nullable SchemaType getSchemaType(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        return extension.equals("yml") || extension.equals("yaml")
                ? SchemaType.YAML
                : extension.equals("json")
                        ? SchemaType.JSON
                        : null;
    }

}

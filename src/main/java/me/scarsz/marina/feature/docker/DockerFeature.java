package me.scarsz.marina.feature.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import me.scarsz.marina.Command;
import me.scarsz.marina.exception.InsufficientPermissionException;
import me.scarsz.marina.feature.AbstractFeature;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.interactions.commands.Command.Choice;

public class DockerFeature extends AbstractFeature {

    private final DockerClient dockerClient;

    public DockerFeature() {
        super();
        this.dockerClient = DockerClientBuilder.getInstance().build();

        getJda().upsertCommand("container", "Manage containers")
                .addSubcommands(new SubcommandData("list", "List containers you have permission for")
                        .addOption(OptionType.STRING, "name", "Name to filter containers by")
                )
                .addSubcommands(new SubcommandData("restart", "Restart a container")
                        .addOption(OptionType.STRING, "container", "Container name to restart", false, true)
                )
                .addSubcommands(new SubcommandData("update", "Update a container")
                        .addOption(OptionType.STRING, "container", "Container name to update", true, true)
                )
                .queue();
    }

    @Command(name = "container.list")
    public void listCommand(SlashCommandInteractionEvent event) {
        OptionMapping nameOption = event.getOption("name");
        Set<String> containers = listContainers(nameOption != null ? nameOption.getAsString() : null, event.getUser()).stream()
                .map(this::getContainerName)
                .collect(Collectors.toSet());

        if (containers.size() > 0) {
            event.getHook().editOriginal("✅ Available containers: `" + String.join("`, `", containers) + "`").queue();
        } else {
            event.getHook().editOriginal("❌ You don't have access to any containers.").queue();
        }
    }
    @Command(name = "container.restart")
    public void restartCommand(SlashCommandInteractionEvent event) throws IllegalArgumentException, InsufficientPermissionException {
        OptionMapping containerOption = event.getOption("container");
        if (containerOption != null) {
            String container = containerOption.getAsString();

            checkPermission(event.getUser(), "docker.container." + container.replace("-", ".") + ".lifecycle");
            inspectContainer(container);

            event.getHook().editOriginal("🤔 Restarting `" + container + "`").complete();
            dockerClient.restartContainerCmd(container).withtTimeout(30).exec();
            event.getHook().editOriginal("✅ Restarted `" + container + "`").complete();
        } else {
            List<Container> containers = listContainers(null, event.getUser());
            event.getHook().editOriginalComponents(ActionRow.of(StringSelectMenu.create("list-" + event.getChannel().getId() + "-" + event.getUser().getId())
                    .setPlaceholder("Which container do you want to restart?")
                    .setRequiredRange(1, 1)
                    .addOptions(containers.stream().map(container ->
                            SelectOption.of("restart-" + container.getId(), getContainerName(container.getId()))
                                    .withDescription(container.getImage())
                            )
                            .collect(Collectors.toSet())
                    )
                    .build()
            )).queue();
        }
    }
    @Command(name = "container.update")
    public void updateCommand(SlashCommandInteractionEvent event) throws IllegalArgumentException, InsufficientPermissionException {
        String container = event.getOption("container").getAsString(); //TODO allow null container & selections
        checkPermission(event.getUser(), "docker.container." + container.replace("-", ".") + ".update");
        InspectContainerResponse preInspection = inspectContainer(container);

        try {
            dockerClient.inspectImageCmd("containrrr/watchtower").exec();
        } catch (NotFoundException e) {
            event.getHook().editOriginal("🤔 Updater image doesn't exist, pulling...").complete();

            try {
                dockerClient.pullImageCmd("containrrr/watchtower").start().awaitCompletion(30, TimeUnit.SECONDS);
            } catch (InterruptedException e2) {
                event.getHook().editOriginal("❌ Timed out while pulling updater image").complete();
                e2.printStackTrace();
                return;
            }
        }

        event.getHook().editOriginal("🤔 Creating updater container...").complete();
        CreateContainerResponse updateContainerCreateResponse = dockerClient.createContainerCmd("containrrr/watchtower")
                .withCmd("--cleanup", "--run-once", container)
                .withHostConfig(HostConfig.newHostConfig()
                        .withAutoRemove(true)
                        .withBinds(
                                Bind.parse("/var/run/docker.sock:/var/run/docker.sock"),
                                Bind.parse("/home/" + System.getenv("DOCKER_USER") + "/.docker/config.json:/config.json:ro")
                        )
                )
                .exec();

        event.getHook().editOriginal("🤔 Updating...").complete();
        dockerClient.startContainerCmd(updateContainerCreateResponse.getId()).exec();
        dockerClient.waitContainerCmd(updateContainerCreateResponse.getId()).exec(new WaitContainerResultCallback() {
            @Override
            public void onComplete() {
                InspectContainerResponse postInspection = inspectContainer(container);

                if (preInspection.getId().equals(postInspection.getId())) {
                    event.getHook().editOriginal("❌ No newer image found for `" + container + "@" + preInspection.getImageId() + "`").queue();
                } else {
                    event.getHook().editOriginal("✅ Updated `" + container + "`").queue();
                }
            }
        });
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        String[] commandPath = event.getFullCommandName().split(" ");
        AutoCompleteQuery focusedOption = event.getFocusedOption();

        if (!"container".equals(commandPath[0])) {
            return; // not under the "container" command
        }

        if (!("update".equals(commandPath[1]) || "restart".equals(commandPath[1]))) {
            return; // not the "update" or "restart" subcommands
        }

        if (!"container".equals(focusedOption.getName())) {
            return; // not the "container" option
        }

        String optionValue = focusedOption.getValue().isBlank() ? null : focusedOption.getValue();
        List<Choice> options = listContainers(optionValue, event.getUser()).stream()
                .map(this::getContainerName)
                .map(word -> new Choice(word, word)) // map the words to choices
                .collect(Collectors.toList());
        event.replyChoices(options).queue();
    }

//    @Override
//    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
//        for (String value : event.getValues()) {
//            TODO selection from commands that didn't specify container
//        }
//    }


    private List<Container> listContainers(String nameFilter) {
        ListContainersCmd cmd = dockerClient.listContainersCmd();
        if (nameFilter != null) cmd.withNameFilter(Collections.singletonList(nameFilter));
        return cmd.exec();
    }
    private List<Container> listContainers(String nameFilter, ISnowflake snowflake) {
        return listContainers(nameFilter).stream()
                .filter(container -> hasPermission(snowflake, "docker.container." + getContainerName(container).replace("-", ".") + ".inspect"))
                .collect(Collectors.toList());
    }
    private InspectContainerResponse inspectContainer(String container) throws IllegalArgumentException {
        try {
            return dockerClient.inspectContainerCmd(container).exec();
        } catch (NotFoundException e) {
            throw new IllegalArgumentException("Container `" + container + "` doesn't exist");
        }
    }

    private String getContainerName(Container container) {
        String name = container.getNames()[0].toLowerCase(Locale.ROOT);
        if (name.startsWith("/")) name = name.substring(1);
        return name;
    }
    private String getContainerName(String s) {
        return inspectContainer(s).getName().toLowerCase(Locale.ROOT);
    }

}

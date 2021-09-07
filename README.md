# Marina
Yet another project management Discord bot.

# Features
- [Manages Docker containers on the host machine.](#managing-docker-containers)
  - Intended for communities that make extensive use of Docker containers to deploy services.

# Managing Docker containers
This bot provides a global `/container <list/start/restart/stop/update> <container>` command on Discord.
Containers on the host machine can be interacted with by Discord users based on bot permissions.

### Example of updating a container named `discordsrv-heads`
1. User executes `/container update discordsrv-heads`
2. Bot checks if user has permission to interact with this container, in the following order:
   1. `docker.container.discordsrv.heads`
   2. `docker.container.discordsrv`
   3. `docker.container`
   4. `docker`
   - Permission checks are skipped for users defined in the `SUPERUSERS` environment variable
3. If the user has permission, [update the target container using `containrrr/watchtower`](https://github.com/containrrr/watchtower)

I prefix my Docker containers with `prefix-` for my different services. This works well with the above permission system
because:
- A group of trusted contributors a certain group of containers can just be granted the `docker.container.[group]`
permission to have access to manage all of that group's Docker containers
- Individual permissions can be granted on a per-situation scenario using `docker.container.discordsrv.heads` if a contributor only needed access to `discordsrv-heads`, for example.

# Deployment

See included [`docker-compose.yml`](https://github.com/Scarsz/Marina/blob/master/docker-compose.yml)
and [`.env`](https://github.com/Scarsz/Marina/blob/master/example.env) files.

1. Create a new (private) Discord bot at https://discord.com/developers/applications
2. Build the bot image with `docker build -t scarsz/marina https://github.com/Scarsz/Marina.git`
   - Optionally uncomment the `build` directive in the included docker-compose.yml to have Compose build the image
   - Won't be necessary once I get around to adding the bot to a registry
3. Save [docker-compose.yml](https://github.com/Scarsz/Marina/blob/master/docker-compose.yml) to a fresh directory
4. Save [example.env](https://github.com/Scarsz/Marina/blob/master/example.env) as .env and fill in your values
   - `TOKEN` Bot token Marina should use.
   - `SUPERUSERS` Comma-separated list of user IDs that always pass permission checks within the bot.
   - `LOGGING_CHANNEL` Channel ID of a logging channel for the bot.
   - `GITHUB_CLIENT` / `GITHUB_SECRET` GitHub application credentials, used when making GitHub API calls.
   - `DOCKER_USER` User on host machine that has logged-in registry credentials. **Only necessary when updating containers using private images.**
     - Used when updating containers as volume mount `/home/DOCKER_USER/.docker/config.json`
5. Start the stack with `docker-compose up` and ensure bot starts successfully
   - Optionally start with `docker-compose up -d && docker-compose logs -f marina` to be able to Ctrl+C without stopping

package me.scarsz.marina;

import com.github.kevinsawicki.http.HttpRequest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class Main {

    public static void main(String[] args) throws Exception {
        if (StringUtils.isBlank(System.getenv("TOKEN"))) {
            Marina.getLogger().error("Bot TOKEN environment variable is not set, can't start");
            System.exit(1);
        }

        // set connection factory for http requests
        // includes functionality for GITHUB_CLIENT & GITHUB_SECRET for GitHub API requests
        String githubClient = System.getenv("GITHUB_CLIENT");
        String githubSecret = System.getenv("GITHUB_SECRET");
        if (StringUtils.isNotBlank(githubClient) && StringUtils.isNotBlank(githubSecret)) {
            HttpRequest.setConnectionFactory(new HttpRequest.ConnectionFactory() {
                public HttpURLConnection create(URL url) throws IOException {
                    return create(url, null);
                }
                public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
                    HttpURLConnection connection = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));
                    connection.setRequestProperty("User-Agent", "Marina");
                    if (url.toString().contains("https://api.github.com")) {
                        connection.setRequestProperty("Authorization", "Basic " + Base64.encodeBase64String((githubClient + ":" + githubSecret).getBytes()));
                    }
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    return connection;
                }
            });
        } else {
            Marina.getLogger().warn("GITHUB_CLIENT/GITHUB_SECRET environment variables are not set, this may lead to being rate limited!");
        }

        String dockerUser = System.getenv("DOCKER_USER");
        if (StringUtils.isBlank(dockerUser)) {
            Marina.getLogger().warn("DOCKER_USER environment variable is not set, Marina might only be able to pull from public registries");
        }

        new Marina();
    }

}

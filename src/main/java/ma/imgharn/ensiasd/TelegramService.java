package ma.imgharn.ensiasd;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sends Telegram notifications through the official Bot API.
 */
public final class TelegramService {

    private final Config config;
    private final HttpClient httpClient;

    public TelegramService(Config config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.requestTimeout())
                .build();
    }

    public boolean isEnabled() {
        return config.isTelegramEnabled();
    }

    public void sendNewArticleNotification(Article article, Instant notificationTime)
            throws IOException, InterruptedException {
        config.requireTelegramConfiguration();

        String message = buildMessage(article, notificationTime);
        String endpoint = "https://api.telegram.org/bot" + config.telegramToken() + "/sendMessage";

        Map<String, String> form = new LinkedHashMap<>();
        form.put("chat_id", config.telegramChatId());
        form.put("text", message);
        form.put("disable_web_page_preview", "true");

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(config.requestTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Telegram API returned HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private String buildMessage(Article article, Instant notificationTime) {
        return String.join(System.lineSeparator(),
                "\uD83D\uDEA8 ENSIASD",
                "",
                "A new article has been published.",
                "",
                "Title:",
                article.title(),
                "",
                "Link:",
                article.url(),
                "",
                "Time:",
                notificationTime.toString()
        );
    }

    private String encodeForm(Map<String, String> form) {
        return form.entrySet()
                .stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

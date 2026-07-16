package ma.imgharn.ensiasd;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Centralized application configuration loaded from environment variables.
 */
public final class Config {

    private static final String DEFAULT_TARGET_URL = "https://ensiasd.uiz.ac.ma/";
    private static final String DEFAULT_STATE_FILE = "src/main/resources/state.json";
    private static final String DEFAULT_USER_AGENT = "ensiasd-monitor/1.0";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final URI targetUri;
    private final Path stateFile;
    private final String telegramToken;
    private final String telegramChatId;
    private final String smtpHost;
    private final String smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String emailFrom;
    private final String emailTo;
    private final String userAgent;
    private final Duration requestTimeout;

    private Config(
            URI targetUri,
            Path stateFile,
            String telegramToken,
            String telegramChatId,
            String smtpHost,
            String smtpPort,
            String smtpUsername,
            String smtpPassword,
            String emailFrom,
            String emailTo,
            String userAgent,
            Duration requestTimeout
    ) {
        this.targetUri = targetUri;
        this.stateFile = stateFile;
        this.telegramToken = telegramToken;
        this.telegramChatId = telegramChatId;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.emailFrom = emailFrom;
        this.emailTo = emailTo;
        this.userAgent = userAgent;
        this.requestTimeout = requestTimeout;
    }

    public static Config fromEnvironment() {
        Map<String, String> env = System.getenv();

        return new Config(
                URI.create(valueOrDefault(env.get("TARGET_URL"), DEFAULT_TARGET_URL)),
                Path.of(valueOrDefault(env.get("STATE_FILE"), DEFAULT_STATE_FILE)),
                blankToNull(env.get("TELEGRAM_TOKEN")),
                blankToNull(env.get("TELEGRAM_CHAT_ID")),
                blankToNull(env.get("SMTP_HOST")),
                blankToNull(env.get("SMTP_PORT")),
                blankToNull(env.get("SMTP_USERNAME")),
                blankToNull(env.get("SMTP_PASSWORD")),
                blankToNull(env.get("EMAIL_FROM")),
                blankToNull(env.get("EMAIL_TO")),
                valueOrDefault(env.get("USER_AGENT"), DEFAULT_USER_AGENT),
                DEFAULT_TIMEOUT
        );
    }

    public URI targetUri() {
        return targetUri;
    }

    public Path stateFile() {
        return stateFile;
    }

    public String telegramToken() {
        return telegramToken;
    }

    public String telegramChatId() {
        return telegramChatId;
    }

    public String smtpHost() {
        return smtpHost;
    }

    public int smtpPort() {
        try {
            return Integer.parseInt(smtpPort);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("SMTP_PORT must be a valid integer.", exception);
        }
    }

    public String smtpUsername() {
        return smtpUsername;
    }

    public String smtpPassword() {
        return smtpPassword;
    }

    public String emailFrom() {
        return emailFrom;
    }

    public String emailTo() {
        return emailTo;
    }

    public String userAgent() {
        return userAgent;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public boolean isTelegramEnabled() {
        return telegramToken != null && telegramChatId != null;
    }

    public boolean isEmailEnabled() {
        return smtpHost != null
                && smtpPort != null
                && smtpUsername != null
                && smtpPassword != null
                && emailFrom != null
                && emailTo != null;
    }

    public boolean hasPartialEmailConfiguration() {
        boolean anyEmailSetting = smtpHost != null
                || smtpPort != null
                || smtpUsername != null
                || smtpPassword != null
                || emailFrom != null
                || emailTo != null;
        return anyEmailSetting && !isEmailEnabled();
    }

    public void requireTelegramConfiguration() {
        if (!isTelegramEnabled()) {
            throw new IllegalStateException(
                    "TELEGRAM_TOKEN and TELEGRAM_CHAT_ID must be configured to send a notification."
            );
        }
    }

    public void requireEmailConfiguration() {
        if (!isEmailEnabled()) {
            throw new IllegalStateException(
                    "SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, EMAIL_FROM and EMAIL_TO must be configured to send an email notification."
            );
        }

        int port = smtpPort();
        if (port < 1 || port > 65535) {
            throw new IllegalStateException("SMTP_PORT must be between 1 and 65535.");
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        String trimmed = blankToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

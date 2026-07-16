package ma.imgharn.ensiasd;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.time.Instant;
import java.util.Properties;

/**
 * Sends email notifications with Jakarta Mail.
 */
public final class EmailService {

    private final Config config;

    public EmailService(Config config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.isEmailEnabled();
    }

    public void sendNewArticleNotification(Article article, Instant notificationTime) throws MessagingException {
        config.requireEmailConfiguration();

        Session session = Session.getInstance(mailProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.smtpUsername(), config.smtpPassword());
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.emailFrom()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.emailTo()));
        message.setSubject("ENSIASD - New article published");
        message.setText(buildMessage(article, notificationTime));

        Transport.send(message);
    }

    private Properties mailProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.smtpHost());
        properties.put("mail.smtp.port", String.valueOf(config.smtpPort()));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", config.smtpPort() == 465 ? "false" : "true");
        properties.put("mail.smtp.ssl.enable", config.smtpPort() == 465 ? "true" : "false");
        properties.put("mail.smtp.connectiontimeout", String.valueOf(config.requestTimeout().toMillis()));
        properties.put("mail.smtp.timeout", String.valueOf(config.requestTimeout().toMillis()));
        properties.put("mail.smtp.writetimeout", String.valueOf(config.requestTimeout().toMillis()));
        return properties;
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
}

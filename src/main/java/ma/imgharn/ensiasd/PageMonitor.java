package ma.imgharn.ensiasd;

import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Coordinates page download, hashing, state persistence and notifications.
 */
public final class PageMonitor {

    private static final Logger LOGGER = Logger.getLogger(PageMonitor.class.getName());
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final Config config;
    private final HashService hashService;
    private final StateRepository stateRepository;
    private final TelegramService telegramService;
    private final EmailService emailService;

    public PageMonitor(
            Config config,
            HashService hashService,
            StateRepository stateRepository,
            TelegramService telegramService,
            EmailService emailService
    ) {
        this.config = config;
        this.hashService = hashService;
        this.stateRepository = stateRepository;
        this.telegramService = telegramService;
        this.emailService = emailService;
    }

    public void checkForChanges() throws IOException {
        List<Article> currentArticles = downloadArticles();
        if (currentArticles.isEmpty()) {
            LOGGER.warning("No articles were extracted from the ENSIASD news section. State was not changed.");
            return;
        }

        String normalizedArticles = normalizeArticles(currentArticles);
        String currentHash = hashService.sha256(normalizedArticles);
        State currentState = new State(currentHash, currentArticles);
        State previousState = stateRepository.read(config.stateFile()).orElse(State.empty());

        if (previousState.isEmpty()) {
            stateRepository.save(config.stateFile(), currentState);
            LOGGER.info(() -> "State initialized with " + currentArticles.size() + " articles. No notification sent.");
            return;
        }

        List<Article> newArticles = findNewArticles(previousState.articles(), currentArticles);
        if (newArticles.isEmpty()) {
            if (!currentHash.equals(previousState.hash())) {
                stateRepository.save(config.stateFile(), currentState);
                LOGGER.info("Article list changed without new article URLs. State updated without notification.");
            } else {
                LOGGER.info("No new ENSIASD article detected.");
            }
            return;
        }

        sendNotifications(newArticles);
        stateRepository.save(config.stateFile(), currentState);
        LOGGER.info(() -> "State updated after detecting " + newArticles.size() + " new "
                + (newArticles.size() == 1 ? "article." : "articles."));
    }

    private List<Article> downloadArticles() throws IOException {
        Connection.Response response = Jsoup.connect(config.targetUri().toString())
                .userAgent(config.userAgent())
                .timeout(Math.toIntExact(config.requestTimeout().toMillis()))
                .followRedirects(true)
                .execute();

        String html = new String(response.bodyAsBytes(), StandardCharsets.UTF_8);
        Document document = Jsoup.parse(html, config.targetUri().toString());
        return extractArticles(document);
    }

    List<Article> extractArticles(Document document) {
        Element newsSection = findNewsSection(document);
        Elements articleCards = newsSection == null
                ? document.select(".ue_post_carousel_item:has(.uc_post_title)")
                : newsSection.select(".ue_post_carousel_item:has(.uc_post_title)");

        Map<String, Article> articlesByUrl = new LinkedHashMap<>();
        for (Element articleCard : articleCards) {
            extractArticle(articleCard).stream()
                    .filter(article -> !article.title().isBlank())
                    .filter(article -> !article.url().isBlank())
                    .forEach(article -> articlesByUrl.putIfAbsent(article.url(), article));
        }

        return List.copyOf(articlesByUrl.values());
    }

    private Element findNewsSection(Document document) {
        for (Element heading : document.select("h1, h2, h3, h4")) {
            String text = normalizeForMatching(heading.text());
            if (text.contains("actualites") && text.contains("evenements")) {
                Element section = closestElementorTopSection(heading);
                return section == null ? heading.closest("section") : section;
            }
        }
        return null;
    }

    private Element closestElementorTopSection(Element element) {
        Element current = element;
        while (current != null) {
            if ("section".equals(current.tagName()) && current.hasClass("elementor-top-section")) {
                return current;
            }
            current = current.parent();
        }
        return null;
    }

    private java.util.Optional<Article> extractArticle(Element articleCard) {
        Element titleElement = articleCard.selectFirst(".uc_post_title");
        Element linkElement = articleCard.selectFirst("a:has(.uc_post_title)[href]");

        if (linkElement == null) {
            linkElement = articleCard.selectFirst(".uc_more_btn[href]");
        }

        if (titleElement == null || linkElement == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new Article(
                normalizeText(titleElement.text()),
                normalizeText(linkElement.absUrl("href"))
        ));
    }

    private List<Article> findNewArticles(List<Article> previousArticles, List<Article> currentArticles) {
        Set<String> previousUrls = previousArticles.stream()
                .map(Article::url)
                .collect(Collectors.toSet());

        List<Article> newArticles = new ArrayList<>();
        for (Article article : currentArticles) {
            if (!previousUrls.contains(article.url())) {
                newArticles.add(article);
            }
        }
        return newArticles;
    }

    private String normalizeArticles(List<Article> articles) {
        return articles.stream()
                .map(Article::normalizedLine)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private void sendNotifications(List<Article> newArticles) {
        boolean telegramEnabled = telegramService.isEnabled();
        boolean emailEnabled = emailService.isEnabled();

        if (!telegramEnabled) {
            LOGGER.warning("Telegram notification skipped because TELEGRAM_TOKEN or TELEGRAM_CHAT_ID is missing.");
        }

        if (config.hasPartialEmailConfiguration()) {
            LOGGER.warning("Email notification skipped because SMTP/email environment variables are incomplete.");
        } else if (!emailEnabled) {
            LOGGER.info("Email notification skipped because SMTP/email environment variables are not configured.");
        }

        for (Article article : newArticles) {
            Instant notificationTime = Instant.now();
            if (telegramEnabled) {
                sendTelegramNotification(article, notificationTime);
            }
            if (emailEnabled) {
                sendEmailNotification(article, notificationTime);
            }
        }
    }

    private void sendTelegramNotification(Article article, Instant notificationTime) {
        try {
            telegramService.sendNewArticleNotification(article, notificationTime);
            LOGGER.info(() -> "Telegram notification sent for article: " + article.url());
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Telegram notification failed for article: " + article.url(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Telegram notification was interrupted for article: " + article.url(), exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Telegram notification failed for article: " + article.url(), exception);
        }
    }

    private void sendEmailNotification(Article article, Instant notificationTime) {
        try {
            emailService.sendNewArticleNotification(article, notificationTime);
            LOGGER.info(() -> "Email notification sent for article: " + article.url());
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Email notification failed for article: " + article.url(), exception);
        }
    }

    private String normalizeText(String value) {
        return WHITESPACE.matcher(value == null ? "" : value).replaceAll(" ").trim();
    }

    private String normalizeForMatching(String value) {
        String normalized = Normalizer.normalize(normalizeText(value), Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .toLowerCase(Locale.ROOT);
    }
}

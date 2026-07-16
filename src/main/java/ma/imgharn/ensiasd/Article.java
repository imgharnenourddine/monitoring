package ma.imgharn.ensiasd;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable article data extracted from the ENSIASD news section.
 */
public record Article(String title, String url) {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public Article {
        title = normalize(title);
        url = normalize(url);

        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(url, "url must not be null");
    }

    public String normalizedLine() {
        return title + "|" + url;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return WHITESPACE.matcher(value).replaceAll(" ").trim();
    }
}

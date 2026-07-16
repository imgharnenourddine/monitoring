package ma.imgharn.ensiasd;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * JSON-serializable monitor state.
 */
public record State(String hash, List<Article> articles) {

    public State {
        hash = hash == null ? "" : hash;
        articles = articles == null ? List.of() : List.copyOf(articles);
    }

    public static State empty() {
        return new State("", List.of());
    }

    @JsonIgnore
    public boolean isEmpty() {
        return hash.isBlank() && articles.isEmpty();
    }
}

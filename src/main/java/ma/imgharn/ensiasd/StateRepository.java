package ma.imgharn.ensiasd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes the JSON state used between monitor executions.
 */
public final class StateRepository {

    private static final Logger LOGGER = Logger.getLogger(StateRepository.class.getName());

    private final ObjectMapper objectMapper;

    public StateRepository() {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Optional<State> read(Path stateFile) throws IOException {
        if (!Files.exists(stateFile)) {
            return Optional.empty();
        }

        String content = removeUtf8Bom(Files.readString(stateFile, StandardCharsets.UTF_8)).trim();
        if (content.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(content, State.class));
        } catch (JsonProcessingException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "State file is not valid JSON. It will be replaced with structured article state.",
                    exception
            );
            return Optional.empty();
        }
    }

    public void save(Path stateFile, State state) throws IOException {
        Path parent = stateFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                stateFile,
                objectMapper.writeValueAsString(state) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private String removeUtf8Bom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }
}

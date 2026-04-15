package moaon.backend.search.dictionary.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Value;

@Value
public class NoriEntry {
    int id;
    String surface;
    List<String> segments;
    String raw;

    public static NoriEntry fromLine(int id, String line) {
        String trimmed = line.trim();
        String[] parts = trimmed.split("\\s+");
        String surface = parts[0];
        List<String> segments = parts.length > 1
                ? Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length))
                : Collections.emptyList();
        return new NoriEntry(id, surface, segments, trimmed);
    }

    public String toFileLine() {
        if (segments.isEmpty()) {
            return surface;
        }
        return surface + " " + String.join(" ", segments);
    }
}

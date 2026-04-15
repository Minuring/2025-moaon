package moaon.backend.search.dictionary.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class SynonymEntry {
    int id;
    List<String> terms;
    String raw;

    public static SynonymEntry fromLine(int id, String line) {
        String trimmed = line.trim();
        List<String> terms = Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
        return new SynonymEntry(id, terms, trimmed);
    }

    public String toFileLine() {
        return String.join(", ", terms);
    }
}

package moaon.backend.search.dictionary.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import moaon.backend.search.dictionary.model.SynonymEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class SynonymFileRepository {

    private final Path filePath;

    public SynonymFileRepository(@Value("${dictionary.synonym-path}") String synonymPath) {
        this.filePath = Path.of(synonymPath);
    }

    public List<SynonymEntry> findAll() {
        try {
            if (!Files.exists(filePath)) {
                return new ArrayList<>();
            }
            List<String> lines = Files.readAllLines(filePath);
            List<SynonymEntry> entries = new ArrayList<>();
            int id = 1;
            for (String line : lines) {
                if (!line.isBlank()) {
                    entries.add(SynonymEntry.fromLine(id++, line));
                }
            }
            return entries;
        } catch (IOException e) {
            throw new IllegalStateException("synonym 파일 읽기 실패: " + filePath, e);
        }
    }

    public void saveAll(List<SynonymEntry> entries) {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            String content = entries.stream()
                    .map(SynonymEntry::toFileLine)
                    .collect(Collectors.joining(System.lineSeparator()));
            Files.writeString(filePath, content.isEmpty() ? "" : content + System.lineSeparator());
        } catch (IOException e) {
            throw new IllegalStateException("synonym 파일 저장 실패: " + filePath, e);
        }
    }
}

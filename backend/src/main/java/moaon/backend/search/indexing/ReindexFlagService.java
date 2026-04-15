package moaon.backend.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReindexFlagService {

    private final Path flagPath;

    public ReindexFlagService(@Value("${dictionary.flag-path}") String flagPath) {
        this.flagPath = Path.of(flagPath);
    }

    public void markDirty() {
        try {
            if (flagPath.getParent() != null) {
                Files.createDirectories(flagPath.getParent());
            }
            Files.writeString(flagPath, Instant.now().toString());
            log.info("Reindex flag 생성: {}", flagPath);
        } catch (IOException e) {
            log.error("Reindex flag 생성 실패: {}", flagPath, e);
        }
    }

    public boolean isReindexRequired() {
        return Files.exists(flagPath);
    }
}

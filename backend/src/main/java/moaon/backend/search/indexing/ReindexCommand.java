package moaon.backend.search.indexing;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReindexCommand implements ApplicationRunner {

    private final ArticleIndexer indexer;

    /**
     * java -jar app.jar --reindex --spring.main.web-application-type=none --batchsize=n
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (args.containsOption("reindex")) {
            List<String> batchSizeOptions = args.getOptionValues("batchsize");
            int batchSize = Integer.parseInt(batchSizeOptions.getFirst());
            indexer.indexAll(batchSize);
            System.exit(0);
        }
    }
}

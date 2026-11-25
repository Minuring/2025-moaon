package moaon.backend.search.dictionary.dto;

import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import lombok.Value;

@Value
public class AnalyzedToken {

    String token;
    String type;
    long position;

    public static AnalyzedToken from(AnalyzeToken token) {
        return new AnalyzedToken(token.token(), token.type(), token.position());
    }
}

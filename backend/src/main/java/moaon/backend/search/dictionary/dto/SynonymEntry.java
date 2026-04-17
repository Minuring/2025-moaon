package moaon.backend.search.dictionary.dto;

import java.util.List;
import lombok.Value;
import moaon.backend.search.dictionary.domain.SynonymDictionaryEntry;

@Value
public class SynonymEntry {

    Long id;
    List<String> terms;
    String raw;

    public static SynonymEntry from(SynonymDictionaryEntry entity) {
        return new SynonymEntry(entity.getId(), entity.terms(), entity.getRawExpression());
    }

}

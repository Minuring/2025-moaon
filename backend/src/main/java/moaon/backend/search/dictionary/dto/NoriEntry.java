package moaon.backend.search.dictionary.dto;

import java.util.List;
import lombok.Value;
import moaon.backend.search.dictionary.domain.NoriDictionaryEntry;

@Value
public class NoriEntry {

    Long id;
    String surface;
    List<String> segments;
    String raw;

    public static NoriEntry from(NoriDictionaryEntry entity) {
        return new NoriEntry(entity.getId(), entity.getSurface(), entity.segmentList(), entity.toFileLine());
    }

}

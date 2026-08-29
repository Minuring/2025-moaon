package moaon.backend.article.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ArticleSearchRequest {
    private String sort;
    private List<String> techStacks = new ArrayList<>();
    private String sector;
    private List<String> topics = new ArrayList<>();
    private String search;
    @Min(1)
    @Max(100)
    private int limit = 20;
    private String cursor;

    public boolean hasSort() {
        return sort != null;
    }

    public boolean hasCursor() {
        return cursor != null;
    }

    public boolean hasSearch() {
        return search != null;
    }

    public boolean hasTechStacks() {
        return !techStacks.isEmpty();
    }

    public boolean hasSector() {
        return sector != null;
    }

    public boolean hasTopics() {
        return !topics.isEmpty();
    }
}

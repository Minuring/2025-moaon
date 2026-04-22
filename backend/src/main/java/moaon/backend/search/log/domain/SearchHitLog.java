package moaon.backend.search.log.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SearchHitLog(int rank, Long docId, String title, float score,
        List<String> matchedFields, Map<String, List<String>> snippets) {

    public static SearchHitLog of(int rank, Long docId, String title, float score,
                                   Map<String, List<String>> highlights) {
        List<String> matchedFields = inferMatchedFields(highlights);
        Map<String, List<String>> snippets = extractSnippets(highlights);
        return new SearchHitLog(rank, docId, title, score, matchedFields, snippets);
    }

    private static List<String> inferMatchedFields(Map<String, List<String>> highlights) {
        List<String> fields = new ArrayList<>();
        if (hasHighlight(highlights, "title")) fields.add("title");
        if (hasHighlight(highlights, "summary")) fields.add("summary");
        if (hasHighlight(highlights, "content")) fields.add("content");
        if (fields.isEmpty()) fields.add("content");
        return fields;
    }

    private static Map<String, List<String>> extractSnippets(Map<String, List<String>> highlights) {
        Map<String, List<String>> snippets = new HashMap<>();
        if (hasHighlight(highlights, "title")) snippets.put("title", highlights.get("title"));
        if (hasHighlight(highlights, "summary")) snippets.put("summary", highlights.get("summary"));
        if (hasHighlight(highlights, "content")) snippets.put("content", highlights.get("content"));
        return snippets;
    }

    private static boolean hasHighlight(Map<String, List<String>> highlights, String field) {
        List<String> h = highlights.get(field);
        return h != null && !h.isEmpty() && !h.get(0).isBlank();
    }

    public boolean hasNoTitleOrSummaryMatch() {
        return matchedFields.equals(List.of("content"));
    }
}

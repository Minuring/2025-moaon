package moaon.backend.search.log.domain;

public record FieldMatchStats(int titleCount, int summaryCount, int contentCount, int hitCount) {

    public static FieldMatchStats empty() {
        return new FieldMatchStats(0, 0, 0, 0);
    }
}

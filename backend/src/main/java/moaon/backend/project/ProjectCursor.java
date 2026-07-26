package moaon.backend.project;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProjectCursor<T> {

    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final T sortValue;
    private final Long lastId;

    public T getSortValue() {
        return sortValue;
    }

    public Long getLastId() {
        return lastId;
    }

    public String getNextCursor() {
        if (sortValue instanceof LocalDateTime dateTime) {
            return dateTime.format(CREATED_AT_FORMATTER) + "_" + lastId;
        }
        return sortValue + "_" + lastId;
    }
}

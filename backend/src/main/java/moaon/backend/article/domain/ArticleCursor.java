package moaon.backend.article.domain;

import java.time.LocalDateTime;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.global.util.Parsers;

@Getter
@EqualsAndHashCode
public class ArticleCursor {

    private final Object sortValue;
    private final Long lastId;

    public ArticleCursor(Object sortValue, Long lastId) {
        this.sortValue = sortValue;
        this.lastId = lastId;
    }

    public ArticleCursor(String rawCursor) {
        if (rawCursor == null || !rawCursor.matches("^\\S+_\\d+$")) {
            throw new CustomException(ErrorCode.INVALID_CURSOR_FORMAT);
        }

        String[] split = rawCursor.split("_");
        this.sortValue = split[0];
        this.lastId = Parsers.parseLong(split[1]);
    }

    public <T> T getSortValueAs(Function<Object, T> mapper) {
        return mapper.apply(sortValue);
    }

    public LocalDateTime getSortValueAsLocalDateTime() {
        return Parsers.parseLocalDateTime(sortValue.toString());
    }

    public int getSortValueAsInt() {
        return Parsers.parseInt(sortValue.toString());
    }

    public double getSortValueAsDouble() {
        return Parsers.parseDouble(sortValue.toString());
    }

    public long getSortValueAsLong() {
        return Parsers.parseLong(sortValue.toString());
    }

    @Override
    public String toString() {
        return sortValue + "_" + lastId;
    }
}

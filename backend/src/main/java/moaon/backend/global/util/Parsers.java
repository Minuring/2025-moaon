package moaon.backend.global.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;
import moaon.backend.project.ProjectCursor;

public class Parsers {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)?([a-zA-Z0-9-가-힣]+(\\.[a-zA-Z0-9-가-힣]+)*)(:\\d+)?(/\\S*)?(\\?\\S*)?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final String COUNT_BASED_CURSOR_REGEX = "[0-9]+_[0-9]+";
    private static final String CREATED_AT_CURSOR_REGEX = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?_[0-9]+$";

    private Parsers() {
    }

    public static Double parseDouble(String value) {
        return parse(value, Double::parseDouble);
    }

    public static Integer parseInt(String value) {
        return parse(value, Integer::parseInt);
    }

    public static Long parseLong(String value) {
        return parse(value, Long::parseLong);
    }

    public static LocalDateTime parseLocalDateTime(String value) {
        return parse(value, LocalDateTime::parse);
    }

    public static URL parseURL(String value) {
        if (!URL_PATTERN.matcher(value).matches()) {
            throw new CustomException(ErrorCode.ARGUMENT_NOT_VALID);
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new CustomException(ErrorCode.ARGUMENT_NOT_VALID, e);
        }
    }

    public static <T> ProjectCursor<?> toCursor(
            String cursor,
            Function<String, T> valueParser
    ) {
        if (isCursorEmpty(cursor)) {
            return null;
        }

        String[] valueAndId = splitAndValidateCursorFormat(cursor);

        T sortValue = valueParser.apply(valueAndId[0]);
        Long lastId = parseLong(valueAndId[1]);

        return new ProjectCursor<>(sortValue, lastId);
    }

    private static <T> T parse(String value, Function<String, T> parseFn) {
        if (value == null || value.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_CURSOR_FORMAT);
        }

        try {
            return parseFn.apply(value);
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }

    private static String[] splitAndValidateCursorFormat(String cursor) {
        if (!cursor.matches(COUNT_BASED_CURSOR_REGEX) && !cursor.matches(CREATED_AT_CURSOR_REGEX)) {
            throw new CustomException(ErrorCode.INVALID_CURSOR_FORMAT);
        }

        return cursor.split("_");
    }

    private static boolean isCursorEmpty(String cursor) {
        return cursor == null || cursor.isEmpty();
    }
}

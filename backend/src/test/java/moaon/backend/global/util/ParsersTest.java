package moaon.backend.global.util;

import static moaon.backend.global.exception.custom.ErrorCode.INVALID_CURSOR_FORMAT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.project.domain.ProjectSortType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParsersTest {

    @DisplayName("parseInt: 값이 null 또는 빈 문자열이면 예외를 발생한다.")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void parseInt_throwsWhenValueIsNullOrEmpty(String invalidValue) {
        assertThatThrownBy(() -> Parsers.parseInt(invalidValue))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_CURSOR_FORMAT.getMessage());
    }

    @DisplayName("parseLong: 값이 null 또는 빈 문자열이면 예외를 발생한다.")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void parseLong_throwsWhenValueIsNullOrEmpty(String invalidValue) {
        assertThatThrownBy(() -> Parsers.parseLong(invalidValue))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_CURSOR_FORMAT.getMessage());
    }

    @DisplayName("parseLocalDateTime: 값이 null 또는 빈 문자열이면 예외를 발생한다.")
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void parseLocalDateTime_throwsWhenValueIsNullOrEmpty(String invalidValue) {
        assertThatThrownBy(() -> Parsers.parseLocalDateTime(invalidValue))
                .isInstanceOf(CustomException.class)
                .hasMessage(INVALID_CURSOR_FORMAT.getMessage());
    }

    @DisplayName("toCursor: cursor 형식을 검증한다.")
    @Test
    void cursorParse() {
        // given
        String cursor1 = "2025-09-25T04:35:00.764_35867";
        String cursor2 = "2025-09-25T04:35:00_35867";
        String cursor3 = "2025-09-25T12:13:36.146232141_35867";
        ProjectSortType createdAt = ProjectSortType.CREATED_AT;

        // when - then
        assertDoesNotThrow(() -> createdAt.toCursor(cursor1));
        assertDoesNotThrow(() -> createdAt.toCursor(cursor2));
        assertDoesNotThrow(() -> createdAt.toCursor(cursor3));
    }
}

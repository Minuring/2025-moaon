package moaon.backend.article.domain;

import lombok.RequiredArgsConstructor;
import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;

import java.util.Arrays;

@RequiredArgsConstructor
public enum ArticleSortType {

    CREATED_AT("createdAt"),
    CLICKS("clicks"),
    RELEVANCE("relevance");

    private final String rawExpression;

    private boolean matchesRawExpression(String rawExpression) {
        return this.rawExpression.equalsIgnoreCase(rawExpression);
    }

    public static ArticleSortType from(String rawExpression) {
        if (rawExpression == null) {
            return CREATED_AT;
        }
        return Arrays.stream(ArticleSortType.values())
                .filter(enumValue -> enumValue.matchesRawExpression(rawExpression))
                .findAny()
                .orElseThrow(() -> new CustomException(ErrorCode.SORT_NOT_FOUND));
    }
}

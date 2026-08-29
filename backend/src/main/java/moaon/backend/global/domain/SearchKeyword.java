package moaon.backend.global.domain;

import moaon.backend.global.exception.custom.CustomException;
import moaon.backend.global.exception.custom.ErrorCode;

import java.util.List;
import java.util.regex.Pattern;

public record SearchKeyword(
        String value
) {

    private static final int MAX_LENGTH = 50;

    private static final Pattern SPECIAL_CHARACTERS = Pattern.compile("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s]");

    public SearchKeyword {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_KEYWORD_LENGTH);
        }
    }

    public boolean hasOnlyOneToken() {
        return allTokens().size() == 1;
    }

    public String lastToken() {
        return allTokens().getLast();
    }

    public List<String> allTokensBeforeLastToken() {
        int size = allTokens().size();
        return allTokens().subList(0, size - 1);
    }

    private List<String> allTokens() {
        String[] split = value.trim().split("\\s+");
        return List.of(split);
    }

    public String replaceSpecialCharacters(String replacement) {
        return SPECIAL_CHARACTERS.matcher(value).replaceAll(replacement);
    }

    public static int getMaxLength() {
        return MAX_LENGTH;
    }
}

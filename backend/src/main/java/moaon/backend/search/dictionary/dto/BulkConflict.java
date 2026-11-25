package moaon.backend.search.dictionary.dto;

/**
 * 일괄 추가에서 겹친 줄.
 *
 * @param line         겹친 입력 줄
 * @param duplicate    겹친 term(동의어) 또는 표면어(nori)
 * @param conflictWith 겹친 대상: 기존 항목의 원본 문자열, 또는 같은 입력의 다른 줄
 */
public record BulkConflict(String line, String duplicate, String conflictWith) {
}

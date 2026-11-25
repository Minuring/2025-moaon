package moaon.backend.search.dictionary.dto;

import java.util.List;
import lombok.Value;

@Value
public class AnalyzeResponse {

    // alias가 가리키는 현재 인덱스
    String index;
    // 색인 분석기 결과 (문서를 색인할 때)
    List<AnalyzedToken> indexTokens;
    // 검색 분석기 결과 (검색어를 분석할 때, 동의어 확장 포함)
    List<AnalyzedToken> searchTokens;
    // 색인 분석기에서 사용자 사전만 뺀 결과
    List<AnalyzedToken> noDictionaryTokens;
}

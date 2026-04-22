# 검색 로그 개선 계획

## 현재 구조 요약

```
ArticleQueryCondition (cursor 포함)
  → articleDocumentRepository.search()
  → SearchWithLog(result, SearchLogCapture)      ← 검색 + 로깅 책임 혼재
      SearchLogCapture: query, resultCount, queryTimeMs, hits[]
          SearchHitLog: rank, docId, title, score, matchedFields, snippets
  → BadCaseScoreCalculator → BadCaseScore(score, flags)
  → SearchLogDocument (ES 저장)
      fields: query, searchedAt, resultCount, queryTimeMs, badCaseScore, suspectFlags, searchedDocs[]
```

현재 플래그: `no_result`, `content_only_heavy`, `slow_query`

### 현재 구조의 문제

`ArticleDocumentRepository.search()`가 두 가지 책임을 가짐:
1. ES 쿼리 실행 → `ArticleSearchResult` 조립 (검색)
2. `SearchHitLog` 수집 → `SearchLogCapture` 조립 (로깅)

개선 사항을 추가할수록 이 메서드에 로깅 코드가 계속 쌓임.

---

## 목표 구조 (리팩토링 선행)

로깅 전략을 `SearchLogCaptureAssembler`로 완전 분리한다.

```
ArticleDocumentRepository
  └─ search(condition) → SearchHits<ArticleDocument>   ← 순수 ES I/O만

SearchLogCaptureAssembler                              ← 새 클래스, 로깅 전략의 단일 진입점
  └─ assemble(hits, condition, queryTimeMs) → SearchLogCapture
       ├─ SearchHitLog 수집 (기존)
       ├─ #1 field match stats 집계
       ├─ #2 cursor 여부 추출
       └─ #3 동의어 매칭 수 (선택적)

SearchFacadeImpl                                       ← 오케스트레이션만
  └─ search(condition):
       1. startTime 측정
       2. hits = repository.search(condition)
       3. queryTimeMs 계산
       4. result = toArticleSearchResult(hits, condition)
       5. capture = assembler.assemble(hits, condition, queryTimeMs)
       6. searchLogService.saveAsync(capture)
       7. return result
```

**역할 분담:**
- `ArticleDocumentRepository`: ES I/O만. `SearchLogCapture`, `SearchWithLog` 몰라야 함
- `SearchLogCaptureAssembler`: 로깅 전략 전담. 새 개선 = 이 클래스만 수정
- `ESArticleQueryBuilder`: 쿼리 구성만. 로깅과 무관
- `SearchFacadeImpl`: 흐름 연결만, 로직 없음
- `SearchWithLog` record: 삭제

이 리팩토링을 개선 #1 구현 전에 먼저 진행한다.

---

## 개선 #1 — title/summary/content 매칭 비율 저장

### 동기

`content_only_heavy` 플래그는 "top5 중 60% 이상이 content만 매칭"이라는 단순 boolean.
비율 수치 자체를 저장하면 더 세밀한 분석이 가능하다.

- title 매칭 비율이 낮다 → boost 설정 문제 or 제목 인덱싱 품질 문제 의심
- summary 매칭 비율이 높다 → 요약 품질이 검색에 기여하는지 판단 가능

### 변경 범위

**`SearchLogCaptureAssembler`에서 집계**

`SearchHitLog.matchedFields`를 순회하며 top5 기준 카운트 수집.
`SearchLogCapture`에 `FieldMatchStats` 포함시켜 반환.

```java
public record FieldMatchStats(int titleCount, int summaryCount, int contentCount, int total) {}
```

**`BadCaseScoreCalculator`**

`isContentOnlyHeavy()` 로직은 유지. `FieldMatchStats`를 입력받아 계산하도록 변경.

**`SearchLogDocument`에 필드 추가**
```java
@Field(type = FieldType.Object)
private FieldMatchStats fieldMatchStats;  // top5 기준
```

### 기대 효과

대시보드에서 검색어별로 "title 0건 / summary 2건 / content 3건" 같은 분포를 볼 수 있음.
`content_only_heavy` 플래그는 그대로 유지 (하위 호환).

---

## 개선 #2 — 커서 쿼리 여부 로깅

### 동기

검색 결과에 커서가 포함된다는 것은 2페이지 이상의 요청.
1페이지 결과에 만족하지 못했을 가능성이 있는 신호.
단독으로는 약하지만, 향후 "동일 검색어 + 짧은 시간 내 재검색"과 조합하면 불만족 신호로 사용 가능.

### 변경 범위

**`SearchLogCapture`에 필드 추가**
```java
public record SearchLogCapture(
    String query,
    int resultCount,
    int queryTimeMs,
    List<SearchHitLog> hits,
    boolean hasCursor      // 추가: 커서 있으면 2페이지 이상 요청
) {}
```

**`SearchLogCaptureAssembler.assemble()`에서 커서 여부 추출**
```java
boolean hasCursor = condition.cursor() != null;
```

**`SearchLogDocument`에 필드 추가**
```java
@Field(type = FieldType.Boolean)
private boolean hasCursor;
```

**`BadCaseScoreCalculator`에서 플래그 추가 (선택)**

`hasCursor && resultCount < 5` 같은 조건 조합 시 `few_result_paged` 플래그 추가 가능.
일단은 단순 저장만 하고, 충분한 데이터가 쌓인 후 판단.

### 기대 효과

- 커서 포함 검색어 목록을 뽑아 "사용자가 여러 페이지를 탐색하는 검색어" 파악
- 향후 세션 단위 분석(동일 검색어 반복)의 기반 데이터

---

## 개선 #3 — 동의어 사전 기여 매칭 수 로깅

### 동기

동의어 사전이 실제로 검색 결과에 기여하는지 수치로 확인.
"동의어 덕분에 추가된 결과 N건"이 0이 자주 뜨는 검색어는 사전 확장 후보.

### 구현 방식

ES `explain: true`로 전수 분석하면 정확하지만 성능 비용이 큼.
대신 **ES Analyze API**를 활용한 간접 측정 방식을 사용한다.

**흐름:**
1. 검색 실행 전, 입력 쿼리를 ES Analyze API(`_analyze`)에 보내 확장된 토큰 목록을 조회
2. 원본 쿼리 토큰과 분석 후 토큰을 비교 → 추가된 토큰 = 동의어 확장 토큰
3. 검색 결과 hit들의 highlight 스니펫에서 동의어 확장 토큰이 포함된 문서 수를 카운트

**`SearchLogCapture`에 추가:**
```java
public record SearchLogCapture(
    String query,
    int resultCount,
    int queryTimeMs,
    List<SearchHitLog> hits,
    boolean hasCursor,
    int synonymMatchCount   // 추가: 동의어로 매칭된 hit 수
) {}
```

**`SearchLogDocument`에 추가:**
```java
@Field(type = FieldType.Integer)
private int synonymMatchCount;
```

### 적용 범위 제한 (성능)

Analyze API 호출이 추가되므로, 아래 조건에서만 실행:
- `resultCount < 10` (결과가 적어 동의어 기여가 중요한 케이스)
- 또는 `content_only_heavy` 플래그가 있는 케이스

조건에 해당하지 않으면 `synonymMatchCount = -1` (미집계)로 저장.

### 기대 효과

- `synonymMatchCount = 0` 이면서 `resultCount`도 적은 검색어 → 사전 확장 우선 후보
- 동의어 사전 변경 전후 `synonymMatchCount` 분포 비교로 효과 측정

---

## 구현 순서

| 순서 | 항목 | 난이도 | 비고 |
|------|------|--------|------|
| 0 | 구조 리팩토링 | 낮음 | `SearchLogCaptureAssembler` 분리, `SearchWithLog` 제거 |
| 1 | title/summary/content 비율 | 낮음 | 이미 matchedFields 있음, 집계만 추가 |
| 2 | 커서 쿼리 플래그 | 낮음 | 단순 boolean 전달 |
| 3 | 동의어 매칭 수 | 중간 | Analyze API 연동 필요 |

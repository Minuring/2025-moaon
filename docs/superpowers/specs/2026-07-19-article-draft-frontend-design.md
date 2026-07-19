# 아티클 등록 프론트엔드 연동 설계

## 배경

백엔드가 기존 `GET /crawl` 단일 API(크롤링+LLM 분석 순차 처리, `ArticleContent` url-key 임시저장)에서 `ArticleDraft` 기반 API로 교체되었다 (`docs/article-registration-flow-improvements.md` 참고). 프론트엔드는 아직 이전 API(`/crawl`, `/crawl/token-count`, `POST /articles`의 `url` 필드)를 그대로 사용 중이라 연동이 깨져 있다. 이 문서는 프론트엔드를 새 API 계약에 맞추는 작업만 다룬다.

## 새 백엔드 계약 (참고용, 변경 대상 아님)

```
POST /articles/drafts?url=<url>
  → 201 { draftId: number, title: string }

POST /articles/drafts/{draftId}/analyze
  → 200 { summary: string, sector: string, topics: string(csv), techstacks: string(csv), remainingCount: number }
  (필드별 부분 실패는 빈 문자열로 응답됨 — all-or-nothing 아님)

GET /articles/drafts/quota
  → 200 { remainingCount: number }

POST /articles
  body: [{ projectId, title, summary, techStacks, draftId, sector, topics }]
  (기존 url 필드 → draftId로 교체됨. content는 서버가 draft에서 조회)
```

`increaseCrawlCount`(잔여 횟수 차감)는 `ArticleDraftService.analyze()` 내부에서 **분석 결과가 비어있지 않을 때만** 호출된다. draft 생성 시점에는 차감되지 않는다.

## 현재 프론트엔드 구조 (변경 전)

- `apis/crawl/getCrawlArticle.ts` — `GET /crawl?url=` 단일 호출, `{title, summary, sector, topics, techstacks, remainingCount}` 한번에 응답
- `apis/crawl/crawlArticle.queries.ts` — 위 API를 감싼 단일 mutation
- `apis/articles/getToken.ts` — `GET /crawl/token-count`
- `apis/articles/postArticle.ts`, `articles.type.ts` — `PostArticleData.url: string`
- `domains/components/ArticleSubmission/ArticleForm/hooks/useCrawlArticleMutation.ts` — mutation 결과를 폼에 반영, 실패 시 폼 초기화
- `domains/components/ArticleSubmission/ArticleForm/hooks/useArticleForm.ts` — `isButtonClicked` 하나로 주소를 제외한 전체 필드 잠금/해제, `descriptionToken`(잔여 횟수) 관리, 클릭 시 낙관적 차감 후 실패 시 롤백
- `domains/components/ArticleSubmission/hooks/useArticleSubmission.ts` — 최종 제출 시 `url: article.address` 포함해 `POST /articles` 호출
- `domains/components/ArticleSubmission/types.ts` — `ArticleFormDataType`에 draft 식별자 없음 (`address`가 사실상 유일한 식별 정보)

## 변경 설계

### 1. API 레이어: `apis/crawl` → `apis/articleDrafts`

`apis/crawl/` 디렉터리를 삭제하고 `apis/articleDrafts/`를 신설한다. `apis/articles/getToken.ts`도 삭제한다(quota 조회가 draft 도메인으로 이동).

```
apis/articleDrafts/
  postArticleDraft.ts       // POST /articles/drafts?url=  → ArticleDraftCreateResponse
  postAnalyzeDraft.ts       // POST /articles/drafts/{draftId}/analyze → ArticleDraftAnalyzeResponse
  getDraftQuota.ts          // GET /articles/drafts/quota → DraftQuotaResponse
  articleDrafts.type.ts     // 위 3개 응답 타입
  articleDrafts.queries.ts  // articleDraftsQueries.createDraft() / analyzeDraft() / getQuota()
```

`articleDrafts.type.ts`:

```ts
export interface ArticleDraftCreateResponse {
  draftId: number;
  title: string;
}

export interface ArticleDraftAnalyzeResponse {
  summary: string;
  sector: string;
  topics: string;
  techstacks: string;
  remainingCount: number;
}

export interface DraftQuotaResponse {
  remainingCount: number;
}
```

`articleDrafts.queries.ts`는 기존 `articles.queries.ts`/`crawlArticle.queries.ts` 패턴을 그대로 따른다 (mutationOptions/queryOptions).

### 2. 등록 페이로드 타입 변경

- `apis/articles/articles.type.ts`의 `PostArticleData`: `url: string` 필드를 제거하고 `draftId: number`로 교체.
- `domains/components/ArticleSubmission/types.ts`의 `ArticleFormDataType`에 `draftId?: number` 추가.
  - optional인 이유: `ArticleRegisterPage.tsx`의 `toFormData()`가 이미 등록된 `ProjectArticle`을 `ArticleFormDataType`으로 변환할 때는 draftId가 없음. 이 항목들은 `useArticleSubmission.ts`의 `existingIds` 필터로 최종 제출 대상에서 제외되므로, 제출 시점엔 항상 draftId가 존재함이 보장된다.

### 3. 폼 잠금 상태를 2단계로 분리

`useArticleForm.ts`의 단일 `isButtonClicked`를 `isTitleLocked` / `isRestLocked` 두 상태로 나눈다.

| 상태 | 초기값 | 잠금 해제 시점 | 다시 잠기는 시점 |
|---|---|---|---|
| `isTitleLocked` | `true` | draft 생성(크롤링) 성공 | draft 생성 실패(폼 전체 리셋) |
| `isRestLocked` | `true` | analyze 호출 종료(성공/실패 무관) | draft 생성 실패(폼 전체 리셋) |

`ArticleForm.tsx`에서:
- 제목 `InputFormField`의 `disabled`: `isButtonClicked` → `isTitleLocked`
- 설명 `TextareaFormField`의 `disabled`, `SectorFormField`의 `readOnly`: `isButtonClicked` → `isRestLocked`

`loading`(스피너 표시 조건)은 draft 생성 mutation과 analyze mutation 둘 중 하나라도 `isPending`이면 `true`.

### 4. 검증 버튼 클릭 흐름 (`useCrawlArticleMutation.ts` → `useArticleDraftMutation.ts`로 개명)

```
1. "검증하기" 클릭
2. POST /articles/drafts 호출
   - 실패: 에러 토스트, 폼 전체 초기화(createEmptyFormData), isTitleLocked=true, isRestLocked=true 유지 (여기서 종료)
   - 성공: draftId·title을 폼에 반영, isTitleLocked=false
3. 곧바로 POST /articles/drafts/{draftId}/analyze 호출
   - 실패: 에러 토스트만 표시. draftId·title은 유지. isRestLocked=false (나머지 필드 사용자가 직접 입력)
   - 성공: summary/sector/topics/techstacks 중 값이 있는 필드만 폼에 반영(빈 문자열은 무시 — 기존 병합 로직 유지),
           remainingCount로 잔여 횟수 갱신, isRestLocked=false
```

파일/훅 이름은 더 이상 "crawl"이 아니라 "draft" 개념이므로 함께 개명한다:
- `apis/crawl/*` → `apis/articleDrafts/*` (위 1절)
- `useCrawlArticleMutation.ts` → `useArticleDraftMutation.ts`

### 5. 잔여 횟수(quota) 처리 단순화

기존 `useArticleForm.ts`의 "클릭 시 낙관적으로 1 차감 → 실패하면 이전 값으로 롤백" 로직을 제거한다. 백엔드가 draft 생성 시점에는 카운트를 차감하지 않으므로 낙관적 차감이 실제 상태와 어긋난다. 대신:

- 초기값: `GET /articles/drafts/quota` 조회 결과
- analyze 성공/실패(필드 단위 부분실패 포함) 시 응답에 실린 `remainingCount`로 갱신
- analyze 호출 자체가 실패해 응답이 없는 경우, 표시값을 그대로 둔다 (실제로 차감되지 않았으므로 되돌릴 것도 없음)
- draft 생성(크롤링) 자체가 실패한 경우도 표시값 그대로 둔다

### 6. 최종 제출

`useArticleSubmission.ts`의 `postArticlesClick`에서 페이로드 생성 시 `url: article.address` → `draftId: article.draftId`로 교체. `existingIds` 필터링 이후의 배열이므로 `draftId`는 항상 정의되어 있다. `ArticleFormDataType.draftId`가 optional 타입이므로 페이로드 매핑 시 non-null 단언(`article.draftId!`)을 사용한다.

## 변경 파일 요약

| 구분 | 경로 |
|---|---|
| 삭제 | `apis/crawl/` (전체), `apis/articles/getToken.ts` |
| 신규 | `apis/articleDrafts/postArticleDraft.ts`, `postAnalyzeDraft.ts`, `getDraftQuota.ts`, `articleDrafts.type.ts`, `articleDrafts.queries.ts` |
| 수정 | `apis/articles/articles.type.ts` (`PostArticleData.url`→`draftId`) |
| 수정 | `domains/components/ArticleSubmission/types.ts` (`ArticleFormDataType.draftId?` 추가) |
| 수정 | `domains/components/ArticleSubmission/ArticleForm/hooks/useArticleForm.ts` (잠금 상태 2단계 분리, quota 낙관적 차감 로직 제거) |
| 개명+수정 | `useCrawlArticleMutation.ts` → `useArticleDraftMutation.ts` (2단계 호출 오케스트레이션) |
| 수정 | `domains/components/ArticleSubmission/ArticleForm/ArticleForm.tsx` (잠금 prop 분리 반영) |
| 수정 | `domains/components/ArticleSubmission/hooks/useArticleSubmission.ts` (`url`→`draftId`) |

## 범위 밖

- LLM 응답 안정성 백엔드 개선 (이미 별도로 범위 제외됨)
- `ArticleDraftItem`/`ArticleDraftList` 등 순수 표시 컴포넌트는 `article.title`/`article.description`만 사용하므로 변경 불필요
- 재크롤링(같은 URL 재검증) 시 기존 draft 재사용 여부 — 백엔드 설계상 매번 새 draft 생성이므로 프론트도 매 클릭마다 새로 호출하면 됨 (기존 동작과 동일)

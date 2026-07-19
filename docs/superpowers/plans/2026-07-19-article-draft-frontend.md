# 아티클 등록 프론트엔드 연동 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 프론트엔드의 아티클 등록 플로우를 새 `ArticleDraft` 기반 백엔드 API(`/articles/drafts`, `/articles/drafts/{id}/analyze`, `/articles/drafts/quota`, `POST /articles`의 `draftId`)에 맞춰 연동한다.

**Architecture:** `apis/crawl`을 폐기하고 `apis/articleDrafts`를 신설해 크롤링(draft 생성)과 LLM 분석을 별도 API 호출로 분리한다. `useArticleForm`의 단일 잠금 상태를 제목/나머지 필드 2단계로 나누고, 검증 버튼 클릭 시 draft 생성 → (제목 반영, 제목만 잠금 해제) → 분석 호출 → (나머지 필드 반영, 나머지 필드 잠금 해제) 순으로 처리한다. 최종 등록 페이로드는 `url` 대신 `draftId`를 사용한다.

**Tech Stack:** React 19, TypeScript(strict), TanStack Query v5, Emotion, fetch 기반 자체 `HTTPClient`.

## Global Constraints

- 컴포넌트는 `function` 키워드, 일반 함수는 화살표 함수 (`docs/fe-code-convention.md`)
- 타입 정의는 `interface` 우선
- Styled 컴포넌트는 `S.` 접두어, `.styled.ts` 파일 분리 (이번 작업은 스타일 변경 없음 — 해당 없음)
- import 경로: `@/`, `@domains/`, `@shared/` 별칭 사용 (기존 파일의 절대/상대 경로 패턴을 그대로 따른다 — 같은 도메인 폴더 내부는 상대경로 `./`, `../`, 도메인 경계를 넘을 때는 `@/`, `@domains/` 별칭)
- **테스트 인프라 없음**: `frontend/package.json`의 `test` 스크립트는 placeholder(`exit 1`)이고 `.test.*` 파일이 프로젝트에 하나도 없다. `docs/fe-code-convention.md`도 "테스트: 우선순위 낮음"이라 명시한다. 따라서 이 플랜은 RED-GREEN 테스트 사이클 대신 **`npx tsc --noEmit`(strict 모드)를 각 스텝의 검증 수단으로 사용**한다. 모든 명령은 `frontend/` 디렉터리에서 실행한다.
- 마지막 태스크에서 `pnpm build`(프로덕션 웹팩 빌드)와 `npx biome check`로 한 번 더 전체 검증하고, 개발 서버로 수동 스모크 테스트를 안내한다.
- 백엔드 API 계약은 이미 구현되어 있으며 변경 대상이 아니다 (`docs/article-registration-flow-improvements.md`, `backend/src/main/java/moaon/backend/article/draft/**`).

---

## Task 1: `articleDrafts` API 레이어 신설

**Files:**
- Create: `frontend/src/apis/articleDrafts/articleDrafts.type.ts`
- Create: `frontend/src/apis/articleDrafts/postArticleDraft.ts`
- Create: `frontend/src/apis/articleDrafts/postAnalyzeDraft.ts`
- Create: `frontend/src/apis/articleDrafts/getDraftQuota.ts`
- Create: `frontend/src/apis/articleDrafts/articleDrafts.queries.ts`

**Interfaces:**
- Produces: `ArticleDraftCreateResponse { draftId: number; title: string }`, `ArticleDraftAnalyzeResponse { summary: string; sector: string; topics: string; techstacks: string; remainingCount: number }`, `DraftQuotaResponse { remainingCount: number }`, `articleDraftsQueries.createDraft()` (mutationOptions, `mutationFn: (url: string) => Promise<ArticleDraftCreateResponse>`), `articleDraftsQueries.analyzeDraft()` (mutationOptions, `mutationFn: (draftId: number) => Promise<ArticleDraftAnalyzeResponse>`), `articleDraftsQueries.getQuota()` (queryOptions, `queryKey: ["draftQuota"]`, 결과 타입 `DraftQuotaResponse`).이 파일들은 기존 코드를 전혀 건드리지 않으므로 독립적으로 완결된 태스크다.

- [ ] **Step 1: 응답 타입 정의**

`frontend/src/apis/articleDrafts/articleDrafts.type.ts`:

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

- [ ] **Step 2: draft 생성 API 함수**

`frontend/src/apis/articleDrafts/postArticleDraft.ts`:

```ts
import { httpClient } from "../HttpClient";
import type { ArticleDraftCreateResponse } from "./articleDrafts.type";

const postArticleDraft = async (
  url: string
): Promise<ArticleDraftCreateResponse> => {
  const searchParams = new URLSearchParams();
  searchParams.set("url", url);
  const response = await httpClient.post(
    `/articles/drafts?${searchParams.toString()}`
  );

  if (!response.ok) {
    const { message: errorMessage } = await response.json();
    throw new Error(
      errorMessage ||
        "메타데이터를 가져오는데 실패했어요. 주소를 다시 확인해주세요."
    );
  }

  return response.json();
};

export default postArticleDraft;
```

- [ ] **Step 3: 분석 API 함수**

`frontend/src/apis/articleDrafts/postAnalyzeDraft.ts`:

```ts
import { httpClient } from "../HttpClient";
import type { ArticleDraftAnalyzeResponse } from "./articleDrafts.type";

const postAnalyzeDraft = async (
  draftId: number
): Promise<ArticleDraftAnalyzeResponse> => {
  const response = await httpClient.post(
    `/articles/drafts/${draftId}/analyze`
  );

  if (!response.ok) {
    const { message: errorMessage } = await response.json();
    throw new Error(errorMessage || "아티클 분석에 실패했어요.");
  }

  return response.json();
};

export default postAnalyzeDraft;
```

- [ ] **Step 4: quota 조회 API 함수**

`frontend/src/apis/articleDrafts/getDraftQuota.ts`:

```ts
import { httpClient } from "../HttpClient";
import type { DraftQuotaResponse } from "./articleDrafts.type";

const getDraftQuota = async (): Promise<DraftQuotaResponse> => {
  const response = await httpClient.get(`/articles/drafts/quota`);

  if (!response.ok) {
    throw new Error("남은 횟수를 가져오지 못했습니다.");
  }

  return response.json();
};

export default getDraftQuota;
```

- [ ] **Step 5: TanStack Query 옵션 모음**

`frontend/src/apis/articleDrafts/articleDrafts.queries.ts`:

```ts
import { mutationOptions, queryOptions } from "@tanstack/react-query";
import getDraftQuota from "./getDraftQuota";
import postAnalyzeDraft from "./postAnalyzeDraft";
import postArticleDraft from "./postArticleDraft";

export const articleDraftsQueries = {
  createDraft: () =>
    mutationOptions({
      mutationFn: (url: string) => postArticleDraft(url),
    }),
  analyzeDraft: () =>
    mutationOptions({
      mutationFn: (draftId: number) => postAnalyzeDraft(draftId),
    }),
  getQuota: () =>
    queryOptions({
      queryKey: ["draftQuota"],
      queryFn: () => getDraftQuota(),
      throwOnError: true,
      retry: 0,
    }),
};
```

- [ ] **Step 6: 타입 검증**

Run (in `frontend/`): `npx tsc --noEmit`
Expected: 에러 없음 (신규 파일은 아직 아무 곳에서도 import되지 않으므로 기존 코드에 영향 없음)

- [ ] **Step 7: 커밋**

```bash
git add frontend/src/apis/articleDrafts
git commit -m "feat: articleDrafts API 레이어 신설"
```

---

## Task 2: 최종 등록 페이로드를 `draftId` 기반으로 전환

**Files:**
- Modify: `frontend/src/apis/articles/articles.type.ts`
- Modify: `frontend/src/domains/components/ArticleSubmission/types.ts`
- Modify: `frontend/src/domains/components/ArticleSubmission/hooks/useArticleSubmission.ts`

**Interfaces:**
- Consumes: 없음 (Task 1과 독립)
- Produces: `PostArticleData.draftId: number` (Task 1과 무관하게 `POST /articles` 페이로드 타입을 확정), `ArticleFormDataType.draftId?: number` (Task 3에서 draft 생성 응답을 저장할 필드로 사용)

- [ ] **Step 1: `PostArticleData`의 `url`을 `draftId`로 교체**

`frontend/src/apis/articles/articles.type.ts`의 `PostArticleData` interface를 다음과 같이 수정한다 (다른 interface는 변경 없음):

```ts
export interface PostArticleData {
  projectId: number;
  title: string;
  summary: string;
  techStacks: TechStackKey[];
  draftId: number;
  topics: AllTopicKey[];
  sector: ArticleSectorKey;
}
```

- [ ] **Step 2: `ArticleFormDataType`에 `draftId` 추가**

`frontend/src/domains/components/ArticleSubmission/types.ts`:

```ts
import type { ArticleSectorKey } from "@domains/filter/articleSector";
import type { AllTopicKey } from "@domains/filter/articleTopic";
import type { TechStackKey } from "@domains/filter/techStack";

export interface SectorType {
  value: ArticleSectorKey;
  topics: AllTopicKey[];
  techStacks: TechStackKey[];
}

export interface ArticleFormDataType {
  id: string;
  draftId?: number;
  address: string;
  title: string;
  description: string;
  sector: SectorType;
}
```

`draftId`가 optional인 이유: `ArticleRegisterPage.tsx`가 이미 등록된 `ProjectArticle`을 `ArticleFormDataType`으로 변환할 때(수정 모드로 불러오는 기존 아티클)는 draftId가 없다. 이 항목들은 아래 Step 3의 `existingIds` 필터로 최종 제출 대상에서 항상 제외되므로, 제출 시점엔 `draftId`가 항상 존재함이 보장된다.

- [ ] **Step 3: 제출 페이로드에서 `url` 대신 `draftId` 사용**

`frontend/src/domains/components/ArticleSubmission/hooks/useArticleSubmission.ts`의 `postArticlesClick` 내부 매핑을 수정한다:

```ts
  const postArticlesClick = useCallback(async () => {
    await postArticlesMutation(
      articles
        .filter((article) => !existingIds.current.has(article.id))
        .map((article) => ({
          projectId,
          title: article.title,
          summary: article.description,
          techStacks: article.sector.techStacks,
          draftId: article.draftId as number,
          sector: article.sector.value,
          topics: article.sector.topics,
        }))
    );
  }, [projectId, articles, postArticlesMutation]);
```

(파일의 나머지 부분은 변경하지 않는다. `article.draftId as number`는 `existingIds` 필터를 통과한 항목엔 항상 draftId가 존재한다는 불변식을 타입 단언으로 표현한 것이다.)

- [ ] **Step 4: 타입 검증**

Run (in `frontend/`): `npx tsc --noEmit`
Expected: 에러 없음

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/apis/articles/articles.type.ts frontend/src/domains/components/ArticleSubmission/types.ts frontend/src/domains/components/ArticleSubmission/hooks/useArticleSubmission.ts
git commit -m "refactor: 아티클 등록 페이로드 url을 draftId로 전환"
```

---

## Task 3: 검증 플로우 재구현 (draft 생성 + 분석 2단계, 폼 잠금 분리)

**Files:**
- Create: `frontend/src/domains/components/ArticleSubmission/ArticleForm/hooks/useArticleDraftMutation.ts`
- Delete: `frontend/src/domains/components/ArticleSubmission/ArticleForm/hooks/useCrawlArticleMutation.ts`
- Modify: `frontend/src/domains/components/ArticleSubmission/ArticleForm/hooks/useArticleForm.ts`
- Modify: `frontend/src/domains/components/ArticleSubmission/ArticleForm/ArticleForm.tsx`

**Interfaces:**
- Consumes: `articleDraftsQueries.createDraft()` / `.analyzeDraft()` / `.getQuota()` (Task 1), `ArticleFormDataType.draftId` (Task 2)
- Produces: `useArticleDraftMutation(setFormData)` → `{ runValidation: (url: string, callbacks: RunValidationCallbacks) => Promise<number | undefined>, isPending: boolean }`. `useArticleForm(...)`이 반환하는 객체에서 `isButtonClicked` 필드가 `isTitleLocked` / `isRestLocked` 두 개로 대체된다 — `ArticleForm.tsx`가 이 이름을 그대로 사용한다.

이 세 파일은 하나의 동작(검증 버튼 클릭 시의 2단계 호출과 필드 잠금)을 이루므로 한 태스크로 묶는다: `useArticleDraftMutation`이 반환하는 형태가 바뀌면 `useArticleForm`이, `useArticleForm`이 반환하는 필드명이 바뀌면 `ArticleForm.tsx`가 즉시 컴파일 에러가 나기 때문에 세 파일을 함께 커밋해야 매 커밋이 컴파일 가능한 상태로 유지된다.

- [ ] **Step 1: 2단계 호출을 오케스트레이션하는 훅 작성**

`frontend/src/domains/components/ArticleSubmission/ArticleForm/hooks/useArticleDraftMutation.ts`:

```ts
import { toast } from "@shared/components/Toast/toast";
import { useMutation } from "@tanstack/react-query";
import type { Dispatch, SetStateAction } from "react";
import { articleDraftsQueries } from "@/apis/articleDrafts/articleDrafts.queries";
import type { ArticleSectorKey } from "@/domains/filter/articleSector";
import type { AllTopicKey } from "@/domains/filter/articleTopic";
import type { TechStackKey } from "@/domains/filter/techStack";
import type { ArticleFormDataType } from "../../types";
import { createEmptyFormData } from "../utils/formUtils";

const parseCsv = (value: string | null | undefined) =>
  (value ?? "")
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);

interface RunValidationCallbacks {
  onTitleUnlocked: () => void;
  onAnalysisSettled: () => void;
  onDraftCreationFailed: () => void;
}

export const useArticleDraftMutation = (
  setFormData: Dispatch<SetStateAction<ArticleFormDataType>>
) => {
  const { mutateAsync: createDraft, isPending: isCreatingDraft } = useMutation(
    articleDraftsQueries.createDraft()
  );
  const { mutateAsync: analyzeDraft, isPending: isAnalyzing } = useMutation(
    articleDraftsQueries.analyzeDraft()
  );

  const runValidation = async (
    url: string,
    { onTitleUnlocked, onAnalysisSettled, onDraftCreationFailed }: RunValidationCallbacks
  ): Promise<number | undefined> => {
    let draft: { draftId: number; title: string };
    try {
      draft = await createDraft(url);
    } catch (error) {
      if (error instanceof Error) {
        toast.error(error.message);
      }
      setFormData(createEmptyFormData());
      onDraftCreationFailed();
      return undefined;
    }

    setFormData((prev) => ({
      ...prev,
      draftId: draft.draftId,
      title: draft.title,
    }));
    onTitleUnlocked();

    try {
      const analyzed = await analyzeDraft(draft.draftId);
      const parsedTopics = parseCsv(analyzed.topics) as AllTopicKey[];
      const parsedTechStacks = parseCsv(analyzed.techstacks) as TechStackKey[];

      setFormData((prev) => ({
        ...prev,
        ...(analyzed.summary ? { description: analyzed.summary } : {}),
        ...(analyzed.sector
          ? {
              sector: {
                value: analyzed.sector as ArticleSectorKey,
                topics: parsedTopics,
                techStacks: parsedTechStacks,
              },
            }
          : {}),
      }));
      onAnalysisSettled();
      return analyzed.remainingCount;
    } catch (error) {
      if (error instanceof Error) {
        toast.error(error.message);
      }
      onAnalysisSettled();
      return undefined;
    }
  };

  return { runValidation, isPending: isCreatingDraft || isAnalyzing };
};
```

- [ ] **Step 2: 기존 단일 호출 훅 삭제**

```bash
rm frontend/src/domains/components/ArticleSubmission/ArticleForm/hooks/useCrawlArticleMutation.ts
```

- [ ] **Step 3: `useArticleForm`을 2단계 잠금 상태로 재작성**

`frontend/src/domains/components/ArticleSubmission/ArticleForm/hooks/useArticleForm.ts` 전체를 다음으로 교체한다:

```ts
import { toast } from "@shared/components/Toast/toast";
import { useQuery } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useState } from "react";
import { articleDraftsQueries } from "@/apis/articleDrafts/articleDrafts.queries";
import type { ArticleFormDataType, SectorType } from "../../types";
import {
  type ArticleFormErrors,
  createEmptyFormData,
  validateField,
  validateFormData,
} from "../utils/formUtils";
import { useArticleDraftMutation } from "./useArticleDraftMutation";

interface UseArticleFormProps {
  editingData?: ArticleFormDataType;
  onSubmit: (data: ArticleFormDataType) => void;
  onUpdate: (data: ArticleFormDataType) => void;
  onCancel: () => void;
}

const isEmptyValue = (value: unknown) => {
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === "string") return value.trim().length === 0;
  return value === undefined || value === null;
};

export const useArticleForm = ({
  editingData,
  onSubmit,
  onUpdate,
  onCancel,
}: UseArticleFormProps) => {
  const { data: quota } = useQuery(articleDraftsQueries.getQuota());
  const [descriptionToken, setDescriptionToken] = useState<number>(0);
  const [isTitleLocked, setIsTitleLocked] = useState(true);
  const [isRestLocked, setIsRestLocked] = useState(true);
  const [formData, setFormData] = useState<ArticleFormDataType>(() =>
    createEmptyFormData()
  );
  const [errors, setErrors] = useState<ArticleFormErrors>({});

  const { runValidation, isPending } = useArticleDraftMutation(setFormData);
  const isFormValid = useMemo(() => {
    const validationErrors = validateFormData(formData);
    return Object.values(validationErrors).every((error) => !error);
  }, [formData]);

  useEffect(() => {
    if (quota?.remainingCount !== undefined) {
      setDescriptionToken(quota.remainingCount);
    }
  }, [quota?.remainingCount]);

  useEffect(() => {
    if (!editingData) {
      return;
    }

    setFormData(editingData);
    setIsTitleLocked(false);
    setIsRestLocked(false);
  }, [editingData]);

  const handleMetaDataFetchButtonClick = useCallback(async () => {
    if (!formData.address) {
      toast.warning("아티클 주소를 입력해주세요.");
      return;
    }

    const remainingCount = await runValidation(formData.address, {
      onTitleUnlocked: () => setIsTitleLocked(false),
      onAnalysisSettled: () => setIsRestLocked(false),
      onDraftCreationFailed: () => {
        setIsTitleLocked(true);
        setIsRestLocked(true);
      },
    });

    if (remainingCount !== undefined) {
      setDescriptionToken(remainingCount);
    }
  }, [formData.address, runValidation]);

  const updateFormFieldData = useCallback(
    <K extends keyof ArticleFormDataType>(
      field: K,
      value: ArticleFormDataType[K]
    ) => {
      setFormData((prev) => {
        const next = { ...prev, [field]: value };

        if (isEmptyValue(value)) {
          setErrors((prevErr) => ({ ...prevErr, [field]: undefined }));
          return next;
        }

        const msg = validateField(
          field as keyof ArticleFormErrors,
          value as string,
          next
        );
        setErrors((prevErr) => ({ ...prevErr, [field]: msg || undefined }));
        return next;
      });
    },
    []
  );

  const updateNestedField = useCallback(
    <
      K extends keyof ArticleFormDataType,
      T extends keyof NonNullable<ArticleFormDataType[K]>
    >(
      field: K,
      subField: T,
      subValue: NonNullable<ArticleFormDataType[K]>[T]
    ) => {
      setFormData((prev) => {
        const next = {
          ...prev,
          [field]: {
            ...(prev[field] as SectorType),
            [subField]: subValue,
          },
        };

        if (field === "sector") {
          if (subField === "value") {
            const msg = validateField("sectorValue", subValue as string, next);
            setErrors((prevErr) => ({
              ...prevErr,
              sectorValue: msg || undefined,
            }));

            setErrors((prevErr) => ({
              ...prevErr,
              techStacks: undefined,
              topics: undefined,
            }));
          } else if (subField === "techStacks") {
            if (Array.isArray(subValue) && subValue.length === 0) {
              setErrors((prevErr) => ({ ...prevErr, techStacks: undefined }));
            } else {
              const msg = validateField(
                "techStacks",
                subValue as string[],
                next
              );
              setErrors((prevErr) => ({
                ...prevErr,
                techStacks: msg || undefined,
              }));
            }
          } else if (subField === "topics") {
            if (Array.isArray(subValue) && subValue.length === 0) {
              setErrors((prevErr) => ({ ...prevErr, topics: undefined }));
            } else {
              const msg = validateField("topics", subValue as string[], next);
              setErrors((prevErr) => ({
                ...prevErr,
                topics: msg || undefined,
              }));
            }
          }
        }

        return next;
      });
    },
    []
  );

  const handleSubmit = useCallback(() => {
    const nextErrors = validateFormData(formData);
    setErrors(nextErrors);

    const hasError = Object.values(nextErrors).some((m) => m && m.length > 0);
    if (hasError) {
      return;
    }

    if (onUpdate && editingData) {
      onUpdate(formData);
      setFormData(createEmptyFormData());
      setIsTitleLocked(true);
      setIsRestLocked(true);
      return;
    }

    onSubmit(formData);
    setFormData(createEmptyFormData());
    setIsTitleLocked(true);
    setIsRestLocked(true);
    setErrors({});
  }, [formData, onSubmit, onUpdate, editingData]);

  const handleCancel = useCallback(() => {
    onCancel();
    setFormData(createEmptyFormData());
    setIsTitleLocked(true);
    setIsRestLocked(true);
    setErrors({});
  }, [onCancel]);

  return {
    formData,
    isTitleLocked,
    isRestLocked,
    errors,
    isFormValid,
    descriptionToken,
    updateFormFieldData,
    updateNestedField,
    handleMetaDataFetchButtonClick,
    handleSubmit,
    handleCancel,
    loading: isPending,
  };
};
```

`descriptionToken`의 초기값을 기존 `token?.remainingCount ?? null`(사실상 항상 `any` 타입이라 컴파일 통과했던 잠재 버그 — 쿼리 응답 전에는 `null`이 그대로 노출됨)에서 `0`으로 바꿨다. `TextareaFormField`의 `descriptionToken` prop은 `number` 타입(`null` 불허)이며, `getDraftQuota`가 명시적으로 타입을 갖게 되면서(Task 1) `null` 초기값은 실제 타입 에러가 된다. quota 조회가 끝나면 바로 아래 `useEffect`가 실제 값으로 갱신한다.

- [ ] **Step 4: `ArticleForm.tsx`가 새 필드명을 사용하도록 수정**

`frontend/src/domains/components/ArticleSubmission/ArticleForm/ArticleForm.tsx`에서 구조 분해와 `disabled`/`readOnly` prop을 아래와 같이 수정한다 (그 외 JSX 구조는 동일):

```tsx
  const {
    formData,
    isTitleLocked,
    isRestLocked,
    errors,
    isFormValid,
    descriptionToken,
    updateFormFieldData,
    updateNestedField,
    handleMetaDataFetchButtonClick,
    handleSubmit,
    handleCancel,
    loading,
  } = useArticleForm({
    editingData,
    onSubmit,
    onUpdate,
    onCancel,
  });
```

```tsx
        <InputFormField
          title="아티클 제목"
          name="title"
          placeholder="아티클 제목을 입력해주세요."
          value={formData.title}
          onChange={(e) => updateFormFieldData("title", e.target.value)}
          errorMessage={errors.title}
          disabled={isTitleLocked}
        />
        <TextareaFormField
          title="아티클 한 줄 요약"
          name="description"
          placeholder="아티클 내용을 요약해주세요."
          value={formData.description}
          onChange={(e) => updateFormFieldData("description", e.target.value)}
          errorMessage={errors.description}
          disabled={isRestLocked}
          descriptionToken={descriptionToken}
        />
        <SectorFormField
          sector={formData.sector}
          onChange={(subField, subValue) =>
            updateNestedField("sector", subField, subValue)
          }
          errors={{
            sectorValue: errors.sectorValue,
            techStacks: errors.techStacks,
            topics: errors.topics,
          }}
          readOnly={isRestLocked}
        />
```

- [ ] **Step 5: 타입 검증**

Run (in `frontend/`): `npx tsc --noEmit`
Expected: 에러 없음

- [ ] **Step 6: 커밋**

```bash
git add frontend/src/domains/components/ArticleSubmission/ArticleForm
git commit -m "feat: 아티클 검증 플로우를 draft 생성/분석 2단계로 재구현"
```

---

## Task 4: 레거시 crawl API 제거

**Files:**
- Delete: `frontend/src/apis/crawl/` (디렉터리 전체)
- Delete: `frontend/src/apis/articles/getToken.ts`
- Modify: `frontend/src/apis/articles/articles.queries.ts`

**Interfaces:**
- Consumes: Task 3 완료 후 `apis/crawl/*`와 `apis/articles/getToken.ts`를 참조하는 코드가 하나도 남지 않음 (Task 3에서 `useCrawlArticleMutation.ts` 삭제 및 `useArticleForm.ts`의 `articlesQueries.getToken()` 참조 제거 완료)
- Produces: 없음 (정리 태스크)

- [ ] **Step 1: 미사용 참조 확인**

Run (in `frontend/src`): `grep -rn "apis/crawl\|crawlArticleQueries\|getCrawlArticle\|articlesQueries.getToken" .`
Expected: `apis/crawl/` 내부 파일 자기 자신을 제외하면 결과 없음 (Task 3까지 완료된 상태이므로 외부 참조가 없어야 한다)

- [ ] **Step 2: 레거시 디렉터리/파일 삭제**

```bash
rm -rf frontend/src/apis/crawl
rm frontend/src/apis/articles/getToken.ts
```

- [ ] **Step 3: `articles.queries.ts`에서 `getToken` 관련 코드 제거**

`frontend/src/apis/articles/articles.queries.ts` 전체를 다음으로 교체한다:

```ts
import {
  infiniteQueryOptions,
  mutationOptions,
  queryOptions,
} from "@tanstack/react-query";
import type { ArticleQueryParams, PostArticleData } from "./articles.type";
import getArticles from "./getArticles";
import postArticle from "./postArticle";
import postArticleView from "./postArticleView";

export const articlesQueries = {
  all: ["articles"] as const,
  fetchList: (params: ArticleQueryParams) =>
    infiniteQueryOptions({
      queryKey: [...articlesQueries.all, params],
      queryFn: ({ pageParam }) => getArticles(pageParam, params),
      getNextPageParam: (lastPage) =>
        lastPage.hasNext ? lastPage.nextCursor : "",
      initialPageParam: "",
    }),
  postArticleClick: () =>
    mutationOptions({
      mutationFn: (id: number) => postArticleView(id),
    }),
  postArticles: () =>
    mutationOptions({
      mutationFn: (postData: PostArticleData[]) => postArticle(postData),
    }),
};
```

- [ ] **Step 4: 타입 검증**

Run (in `frontend/`): `npx tsc --noEmit`
Expected: 에러 없음

- [ ] **Step 5: 커밋**

```bash
git add -A frontend/src/apis
git commit -m "chore: 레거시 crawl API 제거"
```

---

## Task 5: 통합 검증

**Files:** 없음 (검증 전용 태스크)

**Interfaces:** 없음

- [ ] **Step 1: 전체 타입 검사**

Run (in `frontend/`): `npx tsc --noEmit`
Expected: 에러 없음

- [ ] **Step 2: 린트 검사**

Run (in `frontend/`): `npx biome check src`
Expected: 에러 없음 (포맷/import 정렬 이슈가 있으면 `npx biome check --write src`로 자동 수정 후 diff 확인)

- [ ] **Step 3: 프로덕션 빌드**

Run (in `frontend/`): `pnpm build`
Expected: 빌드 성공 (webpack 번들링 에러 없음)

- [ ] **Step 4: 수동 스모크 테스트 (개발 서버)**

Run (in `frontend/`): `pnpm dev`

브라우저에서 아티클 등록 페이지(`/article-register?projectId=<유효한 projectId>`)로 이동해 다음을 확인한다:
1. 주소 입력 후 "검증하기" 클릭 → 제목이 먼저 채워지고 제목 필드가 즉시 편집 가능해지는지
2. 잠시 후 요약/직군/기술스택/주제가 채워지고 해당 필드들도 편집 가능해지는지
3. 존재하지 않거나 크롤링 불가능한 URL로 실패 케이스를 재현해 에러 토스트와 폼 초기화가 일어나는지
4. 폼을 채운 뒤 "아티클 추가"로 목록에 담고 "아티클 등록" 클릭 시 실제로 등록되어 프로젝트 상세 페이지로 이동하는지 (백엔드가 로컬에서 함께 떠 있어야 함)

Expected: 위 4가지 모두 설계 문서(`docs/superpowers/specs/2026-07-19-article-draft-frontend-design.md`)의 흐름과 일치

- [ ] **Step 5: 커밋 (수정 사항이 있었던 경우에만)**

Step 2~4에서 코드 수정이 발생했다면 커밋한다:

```bash
git add frontend
git commit -m "fix: 통합 검증 중 발견된 이슈 수정"
```

수정 사항이 없었다면 이 스텝은 생략한다.

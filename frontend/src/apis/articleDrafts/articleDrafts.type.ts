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

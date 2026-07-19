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

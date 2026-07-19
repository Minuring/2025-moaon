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

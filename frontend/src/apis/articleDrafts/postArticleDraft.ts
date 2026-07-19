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

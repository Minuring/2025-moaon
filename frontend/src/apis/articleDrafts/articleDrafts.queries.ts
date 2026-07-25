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

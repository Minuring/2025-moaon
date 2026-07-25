import { infiniteQueryOptions, mutationOptions } from "@tanstack/react-query";
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

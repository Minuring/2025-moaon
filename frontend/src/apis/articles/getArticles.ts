import { httpClient } from "../HttpClient";
import type { ArticleListResponse, ArticleQueryParams } from "./articles.type";

const ARTICLE_PAGE_SIZE = 20;

const getArticles = async (cursor: string, params: ArticleQueryParams): Promise<ArticleListResponse> => {
  const queryString = new URLSearchParams();
  queryString.set("limit", ARTICLE_PAGE_SIZE.toString());
  if (cursor) queryString.set("cursor", cursor);
  if (params.search) queryString.set("search", params.search);
  if (params.sort) queryString.set("sort", params.sort);
  if (params.sector) queryString.set("sector", params.sector);
  if (params.techStacks) queryString.set("techStacks", params.techStacks);
  if (params.topics) queryString.set("topics", params.topics);

  const articles = await httpClient.get(`/articles?${queryString.toString()}`);
  if (!articles.ok) {
    throw new Error("Failed to fetch articles");
  }

  return articles.json();
};

export default getArticles;

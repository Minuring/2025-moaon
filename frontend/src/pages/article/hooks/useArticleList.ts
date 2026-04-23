import { useInfiniteQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router";
import { articlesQueries } from "@/apis/articles/articles.queries";
import useDelayedVisibility from "@/shared/hooks/useDelayedVisibility";

const useArticleList = () => {
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();

  const params = {
    search: searchParams.get("search") ?? "",
    sort: searchParams.get("sort") ?? "",
    sector: searchParams.get("sector") ?? "",
    techStacks: searchParams.get("techStacks") ?? "",
    topics: searchParams.get("topics") ?? "",
  };

  const { data, isLoading, fetchNextPage, isFetchingNextPage } =
    useInfiniteQuery(articlesQueries.fetchList(params));

  const articles = data?.pages.flatMap((page) => page.contents);

  const totalCount = data?.pages[0]?.totalCount ?? 0;

  const hasNext = data?.pages[data.pages.length - 1]?.hasNext ?? false;
  const nextCursor = data?.pages[data.pages.length - 1]?.nextCursor ?? "";

  const scrollEnabled = !isLoading && hasNext && !isFetchingNextPage;

  const showInitialSkeleton = useDelayedVisibility(isLoading);
  const showNextSkeleton = useDelayedVisibility(isFetchingNextPage);
  const showSkeleton = showInitialSkeleton || showNextSkeleton;

  const refetch = async () => {
    await queryClient.resetQueries({
      queryKey: articlesQueries.all,
    });
  };

  return {
    articles,
    hasNext,
    nextCursor,
    totalCount,
    fetchNextPage,
    showSkeleton,
    scrollEnabled,
    refetch,
    isLoading,
  };
};

export default useArticleList;

import useInfiniteScroll from "@shared/hooks/useInfiniteScroll";
import useArticleList from "../hooks/useArticleList";
import * as S from "./ArticleBox.styled";
import ArticleBoxHeader from "./ArticleBoxHeader/ArticleBoxHeader";
import CardList from "./CardList/CardList";

const DEFAULT_SORT_TYPE = "createdAt";

function ArticleBox() {
  const {
    articles,
    hasNext,
    nextCursor,
    totalCount,
    fetchNextPage,
    showSkeleton,
    scrollEnabled,
    isLoading,
  } = useArticleList();
  const { targetRef } = useInfiniteScroll({
    hasNext,
    nextCursor,
    fetchNextPage,
    scrollEnabled,
  });

  return (
    <S.ArticleBoxContainer>
      <ArticleBoxHeader
        totalCount={totalCount}
        isLoading={isLoading}
        initialSort={DEFAULT_SORT_TYPE}
      />
      <CardList
        articles={articles}
        totalCount={totalCount}
        showSkeleton={showSkeleton}
        scrollEnabled={scrollEnabled}
        targetRef={targetRef}
        isLoading={isLoading}
      />
    </S.ArticleBoxContainer>
  );
}

export default ArticleBox;

import { META_TITLE_PREFIX } from "@domains/constants/meta";
import MoveTop from "@shared/components/MoveTop/MoveTop";
import { useMeta } from "@shared/hooks/useMeta";
import ArticleBox from "./ArticleBox/ArticleBox";
import * as S from "./ArticlePage.styled";
import ArticleSearchBar from "./ArticleSearchBar/ArticleSearchBar";

const ARTICLE_PAGE_DESCRIPTION =
  "프로젝트와 관련된 아티클들을 탐색하고 학습하세요";

function ArticlePage() {
  useMeta({
    title: `${META_TITLE_PREFIX}아티클 탐색`,
    description: ARTICLE_PAGE_DESCRIPTION,
  });

  return (
    <S.Main>
      <S.MainBox>
        <S.TitleBox>
          <S.MainTitle tabIndex={0}>아티클 탐색</S.MainTitle>
          <S.MainDescription tabIndex={0}>
            {ARTICLE_PAGE_DESCRIPTION}
          </S.MainDescription>
        </S.TitleBox>
        <ArticleSearchBar />
      </S.MainBox>
      <S.Box>
        <ArticleBox />
      </S.Box>
      <MoveTop />
    </S.Main>
  );
}

export default ArticlePage;

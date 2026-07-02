import { projectArticleQueries } from "@/apis/projectArticles/projectArticle.queries";
import type { ProjectArticle } from "@/apis/projectArticles/projectArticles.type";
import ArticleSubmission from "@domains/components/ArticleSubmission/ArticleSubmission";
import type { ArticleFormDataType } from "@domains/components/ArticleSubmission/types";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router";
import * as S from "./ArticleRegisterPage.styled";

const toFormData = (article: ProjectArticle): ArticleFormDataType => ({
  id: article.id.toString(),
  address: article.url,
  title: article.title,
  description: article.summary,
  sector: {
    value: article.sector,
    topics: article.topics,
    techStacks: article.techStacks,
  },
});

function ArticleRegisterPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const projectId = Number(searchParams.get("projectId"));

  const { data, isPending } = useQuery({
    ...projectArticleQueries.fetchList(projectId),
    enabled: !!projectId,
  });

  if (!projectId) {
    navigate("/mypage");
    return null;
  }

  if (isPending) return null;

  const initialArticles = data?.articles.map(toFormData) ?? [];

  return (
    <S.Container>
      <S.TitleSection>
        <S.Title>아티클 등록</S.Title>
        <S.Description>프로젝트와 관련된 아티클을 등록해주세요</S.Description>
      </S.TitleSection>
      <S.FormBox>
        <ArticleSubmission projectId={projectId} initialArticles={initialArticles} />
      </S.FormBox>
    </S.Container>
  );
}

export default ArticleRegisterPage;

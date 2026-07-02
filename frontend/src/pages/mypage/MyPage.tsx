import { myProjectsQueries } from "@/apis/myProjects/myProjects.queries";
import Card from "@/pages/project-list/CardList/Card/Card";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import * as S from "./MyPage.styled";

function MyPage() {
  const navigate = useNavigate();
  const { data: projects = [] } = useQuery(myProjectsQueries.fetchList());

  return (
    <S.Container>
      <S.Title>마이페이지</S.Title>
      <S.SectionTitle>내 프로젝트</S.SectionTitle>
      {projects.length === 0 ? (
        <S.EmptyMessage>등록된 프로젝트가 없습니다.</S.EmptyMessage>
      ) : (
        <S.ProjectGrid>
          {projects.map((project) => (
            <S.ProjectCardWrapper key={project.id}>
              <Card project={project} />
              <S.AddArticleButton
                onClick={() =>
                  navigate(`/article/register?projectId=${project.id}`)
                }
              >
                아티클 추가
              </S.AddArticleButton>
            </S.ProjectCardWrapper>
          ))}
        </S.ProjectGrid>
      )}
    </S.Container>
  );
}

export default MyPage;

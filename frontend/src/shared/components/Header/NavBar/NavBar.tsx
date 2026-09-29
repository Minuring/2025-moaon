import { useQueryClient } from "@tanstack/react-query";
import { useTabAnimation } from "@shared/hooks/useTabAnimation";
import { useLocation, useNavigate } from "react-router";
import { articlesQueries } from "@/apis/articles/articles.queries";
import { projectQueries } from "@/apis/projects/project.queries";
import * as S from "./NavBar.styled";

const NAV_LIST = [
  {
    id: 1,
    href: "/project",
    text: "프로젝트 탐색",
  },
  {
    id: 2,
    href: "/article",
    text: "아티클 탐색",
  },
  {
    id: 3,
    href: "/wooteco",
    text: "우아한테크코스",
  },
];

function NavBar() {
  const pathname = useLocation().pathname;
  const selectedIndex = NAV_LIST.findIndex((item) => item.href === pathname);
  const { setTabElementsRef } = useTabAnimation({
    selectedIndex,
    duration: 0.3,
  });
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const handleNavigation = (href: string) => {
    navigate(href);

    switch (href) {
      case "/article":
        queryClient.resetQueries({ queryKey: articlesQueries.all });
        break;
      case "/project":
        queryClient.resetQueries({ queryKey: projectQueries.all });
        break;
    }
  };

  return (
    <S.NavBar>
      <S.NavLinkList>
        {NAV_LIST.map(({ id, href, text }, idx) => (
          <S.NavLinkItem key={id} ref={(el) => setTabElementsRef(el, idx)}>
            <S.Link
              type="button"
              isSelected={selectedIndex === idx}
              onClick={() => handleNavigation(href)}
            >
              {text}
            </S.Link>
          </S.NavLinkItem>
        ))}
      </S.NavLinkList>
    </S.NavBar>
  );
}

export default NavBar;

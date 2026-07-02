import styled from "@emotion/styled";
import { BP_768 } from "@/styles/global.styled";

export const Container = styled.div`
  max-width: 72rem;
  margin: 5rem auto;
  padding: 0 1.5rem;
`;

export const Title = styled.h1`
  font-size: 1.75rem;
  font-weight: 600;
  margin-bottom: 2rem;
`;

export const SectionTitle = styled.h2`
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1.25rem;
`;

export const ProjectGrid = styled.ul`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  list-style: none;
  padding: 0;

  ${BP_768} {
    grid-template-columns: 1fr;
  }
`;

export const ProjectCardWrapper = styled.li`
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
`;

export const AddArticleButton = styled.button`
  width: 100%;
  padding: 0.5rem 0;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #ffffff;
  background-color: #2563eb;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;

  &:hover {
    background-color: #1d4ed8;
  }
`;

export const EmptyMessage = styled.p`
  color: #9ca3af;
  font-size: 0.9375rem;
`;

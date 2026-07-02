import { queryOptions } from "@tanstack/react-query";
import getMyProjects from "./getMyProjects";

export const myProjectsQueries = {
  all: ["myProjects"] as const,
  fetchList: () =>
    queryOptions({
      queryKey: myProjectsQueries.all,
      queryFn: getMyProjects,
      throwOnError: true,
    }),
};

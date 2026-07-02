import { httpClient } from "../HttpClient";
import type { MyProject } from "./myProjects.type";

const getMyProjects = async (): Promise<MyProject[]> => {
  const response = await httpClient.get("/projects/me");

  if (!response.ok) {
    throw new Error("Failed to fetch my projects");
  }

  return response.json();
};

export default getMyProjects;

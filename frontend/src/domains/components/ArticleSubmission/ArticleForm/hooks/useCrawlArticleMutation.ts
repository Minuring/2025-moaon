import { toast } from "@shared/components/Toast/toast";
import type { Dispatch, SetStateAction } from "react";
import { crawlArticleQueries } from "@/apis/crawl/crawlArticle.queries";
import type { ArticleSectorKey } from "@/domains/filter/articleSector";
import type { AllTopicKey } from "@/domains/filter/articleTopic";
import type { TechStackKey } from "@/domains/filter/techStack";
import type { ArticleFormDataType } from "../../types";
import { createEmptyFormData } from "../utils/formUtils";

const parseCsv = (value: string | null | undefined) =>
  (value ?? "")
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);

export const useCrawlArticleMutation = (
  setFormData: Dispatch<SetStateAction<ArticleFormDataType>>
) => {
  const { mutateAsync, isPending } = crawlArticleQueries.fetchCrawl();

  const handleFetchAsync = async (
    url: string,
    disabledCondition: (condition: boolean) => void
  ) => {
    try {
      const data = await mutateAsync(url, {
        onSuccess: () => {
          disabledCondition(false);
        },
        onError: () => {
          disabledCondition(true);
          setFormData(createEmptyFormData());
        },
      });

      const { title, summary, sector, topics, techstacks, remainingCount } =
        data;
      const parsedTopics = parseCsv(topics) as AllTopicKey[];
      const parsedTechStacks = parseCsv(techstacks) as TechStackKey[];

      setFormData((prev) => ({
        ...prev,
        ...(title ? { title } : {}),
        ...(summary ? { description: summary } : {}),
        ...(sector
          ? {
              sector: {
                value: sector as ArticleSectorKey,
                topics: parsedTopics,
                techStacks: parsedTechStacks,
              },
            }
          : {}),
      }));

      return { remainingCount };
    } catch (error) {
      if (error instanceof Error) {
        toast.error(error.message);
      }
    }
  };

  return { mutateAsync: handleFetchAsync, isPending };
};

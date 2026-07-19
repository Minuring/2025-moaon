import { toast } from "@shared/components/Toast/toast";
import { useMutation } from "@tanstack/react-query";
import type { Dispatch, SetStateAction } from "react";
import { articleDraftsQueries } from "@/apis/articleDrafts/articleDrafts.queries";
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

interface RunValidationCallbacks {
  onTitleUnlocked: () => void;
  onAnalysisSettled: () => void;
  onDraftCreationFailed: () => void;
}

export const useArticleDraftMutation = (
  setFormData: Dispatch<SetStateAction<ArticleFormDataType>>
) => {
  const { mutateAsync: createDraft, isPending: isCreatingDraft } = useMutation(
    articleDraftsQueries.createDraft()
  );
  const { mutateAsync: analyzeDraft, isPending: isAnalyzing } = useMutation(
    articleDraftsQueries.analyzeDraft()
  );

  const runValidation = async (
    url: string,
    { onTitleUnlocked, onAnalysisSettled, onDraftCreationFailed }: RunValidationCallbacks
  ): Promise<number | undefined> => {
    let draft: { draftId: number; title: string };
    try {
      draft = await createDraft(url);
    } catch (error) {
      if (error instanceof Error) {
        toast.error(error.message);
      }
      setFormData(createEmptyFormData());
      onDraftCreationFailed();
      return undefined;
    }

    setFormData((prev) => ({
      ...prev,
      draftId: draft.draftId,
      title: draft.title,
    }));
    onTitleUnlocked();

    try {
      const analyzed = await analyzeDraft(draft.draftId);
      const parsedTopics = parseCsv(analyzed.topics) as AllTopicKey[];
      const parsedTechStacks = parseCsv(analyzed.techstacks) as TechStackKey[];

      setFormData((prev) => ({
        ...prev,
        ...(analyzed.summary ? { description: analyzed.summary } : {}),
        ...(analyzed.sector
          ? {
              sector: {
                value: analyzed.sector as ArticleSectorKey,
                topics: parsedTopics,
                techStacks: parsedTechStacks,
              },
            }
          : {}),
      }));
      onAnalysisSettled();
      return analyzed.remainingCount;
    } catch (error) {
      if (error instanceof Error) {
        toast.error(error.message);
      }
      onAnalysisSettled();
      return undefined;
    }
  };

  return { runValidation, isPending: isCreatingDraft || isAnalyzing };
};

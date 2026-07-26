package moaon.backend.article.draft.dto;

import moaon.backend.article.draft.ArticleDraft;

public record ArticleDraftCreateResponse(
        Long draftId,
        String title
) {

    public static ArticleDraftCreateResponse from(ArticleDraft draft) {
        return new ArticleDraftCreateResponse(draft.getId(), draft.getCrawledTitle());
    }
}

package moaon.backend.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import org.springframework.util.CollectionUtils;

@Builder
public record ArticleCreateRequest(
        @NotNull Long projectId,
        @NotBlank String title,
        @NotBlank String summary,
        List<String> techStacks,
        @NotNull Long draftId,
        @NotBlank String sector,
        @NotEmpty List<String> topics
) {

    @Override
    public List<String> techStacks() {
        if (CollectionUtils.isEmpty(techStacks)) {
            return new ArrayList<>();
        }
        return techStacks;
    }
}

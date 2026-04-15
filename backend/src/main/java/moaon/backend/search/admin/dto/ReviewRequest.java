package moaon.backend.search.admin.dto;

import jakarta.validation.constraints.NotNull;
import moaon.backend.search.admin.domain.ReviewStatus;

public record ReviewRequest(
    @NotNull ReviewStatus status,
    String memo
) {}

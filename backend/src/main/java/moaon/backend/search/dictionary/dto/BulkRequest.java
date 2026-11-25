package moaon.backend.search.dictionary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BulkRequest {

    @NotBlank
    private String text;
}

package moaon.backend.search.dictionary.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SynonymRequest {
    @NotEmpty
    private List<String> terms;
}

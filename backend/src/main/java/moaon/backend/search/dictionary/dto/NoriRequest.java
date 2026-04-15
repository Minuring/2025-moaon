package moaon.backend.search.dictionary.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class NoriRequest {
    @NotBlank
    private String surface;
    private List<String> segments = new ArrayList<>();
}

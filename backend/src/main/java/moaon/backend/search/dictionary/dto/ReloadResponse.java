package moaon.backend.search.dictionary.dto;

import lombok.Value;

@Value
public class ReloadResponse {
    boolean success;
    String message;
}

package moaon.backend.article.draft.client;

import lombok.Getter;

@Getter
public class AiSummaryFailedException extends RuntimeException {

    private final int responseStatusCode;
    private final String responseMessage;

    public AiSummaryFailedException(int code, String message) {
        super(message);
        this.responseStatusCode = code;
        this.responseMessage = message;
    }
}

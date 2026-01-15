package moaon.backend.global.util;

public class JsonExtractor {

    public static String extractJsonObject(String text) {
        int start = -1;
        int depth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null; // JSON 못 찾은 경우
    }

    private JsonExtractor() {
    }
}

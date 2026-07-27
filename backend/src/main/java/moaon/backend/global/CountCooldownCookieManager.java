package moaon.backend.global;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CountCooldownCookieManager {

    private static final String COUNTED_VALUE = "1";

    public boolean isCountIncreasable(String cookieNamePrefix, long contentId, HttpServletRequest request) {
        return getCookie(cookieNamePrefix, contentId, request) == null;
    }

    public void markAsCounted(String cookieNamePrefix, String cookiePath, long contentId, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName(cookieNamePrefix, contentId), COUNTED_VALUE)
                .path(cookiePath)
                .maxAge(Duration.ofMinutes(10))
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private Cookie getCookie(String cookieNamePrefix, long contentId, HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        String name = cookieName(cookieNamePrefix, contentId);
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private String cookieName(String cookieNamePrefix, long contentId) {
        return cookieNamePrefix + contentId;
    }
}

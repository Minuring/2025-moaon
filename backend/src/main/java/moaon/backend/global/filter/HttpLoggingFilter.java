package moaon.backend.global.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpLoggingFilter implements Filter {

    private static final String LOG_TYPE = "log_type";
    private static final String HTTP_METHOD = "http_method";
    private static final String REQUEST_PATH = "request_path";
    private static final String HTTP_STATUS = "http_status";
    private static final String RESPONSE_TIME_MS = "response_time_ms";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String requestURI = httpServletRequest.getRequestURI();
        if (requestURI.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        doRequestLogging(httpServletRequest);

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
            long responseTime = System.currentTimeMillis() - startTime;
            int status = httpServletResponse.getStatus();
            doResponseLogging(httpServletRequest, status, responseTime);
        } finally {
            MDC.clear();
        }
    }

    private void doRequestLogging(HttpServletRequest httpServletRequest) {
        String method = httpServletRequest.getMethod();
        String requestURI = httpServletRequest.getRequestURI();
        String queryString = httpServletRequest.getQueryString();
        String fullPath = requestURI + (queryString != null ? "?" + queryString : "");

        MDC.put(LOG_TYPE, "http_request");
        MDC.put(HTTP_METHOD, method);
        MDC.put(REQUEST_PATH, fullPath);

        log.info("[REQUEST] {} {}",
                method,
                fullPath
        );
    }

    private void doResponseLogging(HttpServletRequest httpServletRequest, int status, long responseTime) {
        String method = httpServletRequest.getMethod();
        String requestURI = httpServletRequest.getRequestURI();
        String queryString = httpServletRequest.getQueryString();
        String fullPath = requestURI + (queryString != null ? "?" + queryString : "");

        MDC.put(LOG_TYPE, "http_response");
        MDC.put(HTTP_METHOD, method);
        MDC.put(REQUEST_PATH, fullPath);
        MDC.put(HTTP_STATUS, String.valueOf(status));
        MDC.put(RESPONSE_TIME_MS, String.valueOf(responseTime));

        log.info("[RESPONSE] Status: {} | Time(ms): {}",
                status,
                responseTime
        );
    }
}

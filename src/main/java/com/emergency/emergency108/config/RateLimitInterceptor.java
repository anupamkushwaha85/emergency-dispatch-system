package com.emergency.emergency108.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP rate limiter using a fixed sliding-window algorithm (pure Java, no extra dependencies).
 *
 * Limits:
 *  - POST /api/auth/send-otp  → 5 requests / minute  (OTP abuse prevention)
 *  - POST /api/auth/verify-otp → 10 requests / minute (brute-force prevention)
 *  - All other endpoints       → 60 requests / minute (general DDoS mitigation)
 *
 * Returns HTTP 429 with a Retry-After header when the limit is exceeded.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final long WINDOW_MS = 60_000L; // 1 minute

    // Separate caches per tier so each IP has independent counters
    private final ConcurrentHashMap<String, RateLimitWindow> sendOtpWindows   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateLimitWindow> verifyOtpWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateLimitWindow> generalWindows   = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String ip     = resolveClientIp(request);
        String uri    = request.getRequestURI();
        String method = request.getMethod();

        boolean allowed = resolveWindow(ip, uri, method).tryConsume();

        if (allowed) {
            return true;
        }

        log.warn("Rate limit exceeded — IP={} method={} uri={}", ip, method, uri);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Too many requests. Please slow down and retry after 60 seconds.\"}");
        return false;
    }

    // ── Window factory ───────────────────────────────────────────────────────

    private RateLimitWindow resolveWindow(String ip, String uri, String method) {
        if ("POST".equalsIgnoreCase(method) && uri.endsWith("/api/auth/send-otp")) {
            return sendOtpWindows.computeIfAbsent(ip, k -> new RateLimitWindow(5));
        }
        if ("POST".equalsIgnoreCase(method) && uri.endsWith("/api/auth/verify-otp")) {
            return verifyOtpWindows.computeIfAbsent(ip, k -> new RateLimitWindow(10));
        }
        return generalWindows.computeIfAbsent(ip, k -> new RateLimitWindow(60));
    }

    // ── IP resolution ────────────────────────────────────────────────────────

    /**
     * Resolves the real client IP, respecting common reverse-proxy headers.
     * Falls back to {@code getRemoteAddr()} when no forwarded header is present.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Chain format: "client, proxy1, proxy2" — take the leftmost (original client)
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    // ── Fixed-window counter ─────────────────────────────────────────────────

    /**
     * Thread-safe fixed-window rate limit counter.
     * Each instance tracks one IP+tier combination.
     */
    static final class RateLimitWindow {

        private final int limit;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        RateLimitWindow(int limit) {
            this.limit = limit;
        }

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            // If the current window has expired, reset it atomically
            if (now - windowStart > WINDOW_MS) {
                synchronized (this) {
                    if (now - windowStart > WINDOW_MS) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet() <= limit;
        }
    }
}

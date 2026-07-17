package security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final boolean enabled;
  private final int capacity;
  private final long windowMillis;
  private final Set<String> protectedPaths;

  private final ConcurrentHashMap<String, RequestWindow> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(
      @Value("${rate-limit.enabled}") boolean enabled,
      @Value("${rate-limit.capacity}") int capacity,
      @Value("${rate-limit.refill-duration-seconds}") long refillDurationSeconds,
      @Value("${rate-limit.paths}") List<String> protectedPaths) {
    this.enabled = enabled;
    this.capacity = capacity;
    this.windowMillis = refillDurationSeconds * 1000;
    this.protectedPaths = Set.copyOf(protectedPaths);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (!enabled || !protectedPaths.contains(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    String key = request.getRemoteAddr() + ":" + request.getRequestURI();
    if (isRateLimited(key)) {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Too many requests, please try again later.\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isRateLimited(String key) {
    long now = System.currentTimeMillis();
    RequestWindow window = windows.computeIfAbsent(key, k -> new RequestWindow(now));

    synchronized (window) {
      if (now - window.startedAt >= windowMillis) {
        window.startedAt = now;
        window.count.set(0);
      }
      return window.count.incrementAndGet() > capacity;
    }
  }

  private static final class RequestWindow {
    private volatile long startedAt;
    private final AtomicInteger count = new AtomicInteger(0);

    private RequestWindow(long startedAt) {
      this.startedAt = startedAt;
    }
  }
}

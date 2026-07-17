package security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitFilterTest {

  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() throws Exception {
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
    when(response.getWriter()).thenReturn(mock(PrintWriter.class));
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getRequestURI()).thenReturn("/auth/login");
  }

  @Test
  void doFilterInternal_whenDisabled_continuesChainRegardlessOfPath() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(false, 1, 60, List.of("/auth/login"));

    filter.doFilterInternal(request, response, filterChain);
    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(2)).doFilter(request, response);
    verifyNoInteractions(response);
  }

  @Test
  void doFilterInternal_forUnprotectedPath_continuesChainWithoutLimiting() throws Exception {
    when(request.getRequestURI()).thenReturn("/auctions");
    RateLimitFilter filter = new RateLimitFilter(true, 1, 60, List.of("/auth/login"));

    filter.doFilterInternal(request, response, filterChain);
    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(2)).doFilter(request, response);
  }

  @Test
  void doFilterInternal_withinCapacity_continuesChain() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(true, 2, 60, List.of("/auth/login"));

    filter.doFilterInternal(request, response, filterChain);
    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(2)).doFilter(request, response);
  }

  @Test
  void doFilterInternal_exceedingCapacity_rejectsWithTooManyRequests() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(true, 1, 60, List.of("/auth/login"));

    filter.doFilterInternal(request, response, filterChain);
    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(response).setStatus(429);
  }

  @Test
  void doFilterInternal_limitsEachClientIndependently() throws Exception {
    HttpServletRequest otherRequest = mock(HttpServletRequest.class);
    when(otherRequest.getRemoteAddr()).thenReturn("10.0.0.1");
    when(otherRequest.getRequestURI()).thenReturn("/auth/login");

    RateLimitFilter filter = new RateLimitFilter(true, 1, 60, List.of("/auth/login"));

    filter.doFilterInternal(request, response, filterChain);
    filter.doFilterInternal(otherRequest, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(filterChain).doFilter(otherRequest, response);
  }
}

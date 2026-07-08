package security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import domain.user.Role;
import domain.user.TokenGenerator;
import domain.user.TokenGenerator.AccessTokenClaims;
import domain.user.UserExceptions.InvalidRefreshTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private TokenGenerator tokenGenerator;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(tokenGenerator);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_withoutAuthorizationHeader_continuesChainWithoutAuthentication()
      throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(tokenGenerator);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterInternal_withHeaderWithoutBearerPrefix_continuesChainWithoutAuthentication()
      throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic some-credentials");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(tokenGenerator);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterInternal_withValidToken_setsAuthenticationWithCorrectAuthority() throws Exception {
    UUID userId = UUID.randomUUID();
    String token = "valid-token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenGenerator.extractClaimsFromAccessToken(token))
        .thenReturn(new AccessTokenClaims(userId, "test@example.com", Role.BUYER));

    filter.doFilterInternal(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isEqualTo(userId.toString());
    assertThat(authentication.getAuthorities())
        .extracting(Object::toString)
        .containsExactly("ROLE_BUYER");

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_withInvalidToken_continuesChainWithoutPopulatingSecurityContext()
      throws Exception {
    String token = "invalid-token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(tokenGenerator.extractClaimsFromAccessToken(token))
        .thenThrow(new InvalidRefreshTokenException());

    filter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }
}

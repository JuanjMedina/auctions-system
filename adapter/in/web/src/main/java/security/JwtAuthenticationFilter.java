package security;

import domain.user.TokenGenerator;
import domain.user.TokenGenerator.AccessTokenClaims;
import domain.user.UserExceptions.InvalidRefreshTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final TokenGenerator tokenGenerator;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader(AUTHORIZATION_HEADER);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(BEARER_PREFIX.length());

    try {
      AccessTokenClaims claims = tokenGenerator.extractClaimsFromAccessToken(token);

      var authority = new SimpleGrantedAuthority("ROLE_" + claims.role().name());
      var authentication =
          new UsernamePasswordAuthenticationToken(
              claims.userId().toString(), null, List.of(authority));

      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (InvalidRefreshTokenException ignored) {
      // token inválido o expirado — Spring Security rechaza la request en el siguiente filtro
    }

    filterChain.doFilter(request, response);
  }
}

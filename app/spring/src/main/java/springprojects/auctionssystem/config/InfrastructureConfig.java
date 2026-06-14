package springprojects.auctionssystem.config;

import domain.user.Role;
import domain.user.TokenGenerator;
import domain.user.TokenGenerator.AccessTokenClaims;
import domain.user.UserExceptions.InvalidRefreshTokenException;
import domain.user.UserPasswordEncoder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class InfrastructureConfig {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.access-expiration-ms}")
  private long accessExpirationMs;

  @Value("${jwt.refresh-expiration-ms}")
  private long refreshExpirationMs;

  @Bean
  public TokenGenerator tokenGenerator() {
    return new TokenGenerator() {

      @Override
      public String generateAccessToken(UUID userId, String email, Role role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role.name())
            .claim("type", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
            .signWith(key)
            .compact();
      }

      @Override
      public String generateRefreshToken(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
            .signWith(key)
            .compact();
      }

      @Override
      public UUID extractUserIdFromRefreshToken(String refreshToken) {
        try {
          SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
          Claims claims =
              Jwts.parser().verifyWith(key).build().parseSignedClaims(refreshToken).getPayload();

          String type = claims.get("type", String.class);
          if (!"refresh".equals(type)) {
            throw new InvalidRefreshTokenException();
          }

          return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
          throw new InvalidRefreshTokenException();
        }
      }

      @Override
      public AccessTokenClaims extractClaimsFromAccessToken(String accessToken) {
        try {
          SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
          Claims claims =
              Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload();

          String type = claims.get("type", String.class);
          if (!"access".equals(type)) {
            throw new InvalidRefreshTokenException();
          }

          UUID userId = UUID.fromString(claims.getSubject());
          String email = claims.get("email", String.class);
          Role role = Role.valueOf(claims.get("role", String.class));

          return new AccessTokenClaims(userId, email, role);
        } catch (JwtException | IllegalArgumentException e) {
          throw new InvalidRefreshTokenException();
        }
      }
    };
  }

  @Bean
  public UserPasswordEncoder userPasswordEncoder() {
    BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    return new UserPasswordEncoder() {
      public String encode(String raw) {
        return bcrypt.encode(raw);
      }

      public boolean matches(String raw, String encoded) {
        return bcrypt.matches(raw, encoded);
      }
    };
  }
}

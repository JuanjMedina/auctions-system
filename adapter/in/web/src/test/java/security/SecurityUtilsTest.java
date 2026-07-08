package security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void currentUserId_authenticatedPrincipal_returnsParsedUuid() {
    UUID userId = UUID.randomUUID();
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(userId.toString(), "password"));

    UUID result = SecurityUtils.currentUserId();

    assertThat(result).isEqualTo(userId);
  }
}

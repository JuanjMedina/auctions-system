package config;

import domain.user.UserPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PasswordHasherConfig {

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

package springprojects.auctionssystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import shared.PasswordHasher;

@Configuration
public class PasswordHasherConfig {

  @Bean
  public PasswordHasher passwordHasher() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    return encoder::encode;
  }
}

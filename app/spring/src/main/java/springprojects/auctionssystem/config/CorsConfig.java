package springprojects.auctionssystem.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Value("${cors.allowed-origins}")
  private List<String> allowedOrigins;

  @Value("${cors.allowed-methods}")
  private List<String> allowedMethods;

  @Value("${cors.allowed-headers}")
  private List<String> allowedHeaders;

  @Value("${cors.allow-credentials}")
  private boolean allowCredentials;

  @Value("${cors.max-age-seconds}")
  private long maxAgeSeconds;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(allowedMethods);
    configuration.setAllowedHeaders(allowedHeaders);
    configuration.setAllowCredentials(allowCredentials);
    configuration.setMaxAge(maxAgeSeconds);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}

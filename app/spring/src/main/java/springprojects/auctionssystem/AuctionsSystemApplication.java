package springprojects.auctionssystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@ComponentScan(
    basePackages = {
      "springprojects.auctionssystem",
      "controller",
      "adapter",
      "config",
      "exception",
      "usecase"
    })
@EnableJpaRepositories(basePackages = "repository")
@EntityScan(basePackages = "entity")
public class AuctionsSystemApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuctionsSystemApplication.class, args);
  }
}

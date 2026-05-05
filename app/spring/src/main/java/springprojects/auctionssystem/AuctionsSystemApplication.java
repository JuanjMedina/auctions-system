package springprojects.auctionssystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"springprojects.auctionssystem", "controller", "adapter", "config", "exception"})
@EnableJpaRepositories(basePackages = "repository")
@EntityScan(basePackages = "entity")
public class AuctionsSystemApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuctionsSystemApplication.class, args);
  }
}

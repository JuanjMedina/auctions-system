package springprojects.auctionssystem;

import org.springframework.boot.SpringApplication;

public class TestAuctionsSystemApplication {

  public static void main(String[] args) {
    SpringApplication.from(AuctionsSystemApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}

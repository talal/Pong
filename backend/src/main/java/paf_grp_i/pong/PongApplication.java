package paf_grp_i.pong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PongApplication {
  public static void main(String[] args) {
    SpringApplication.run(PongApplication.class, args);
  }
}

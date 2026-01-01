package paf_grp_i.pong;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

@SpringBootApplication
@EnableScheduling
public class PongApplication {

    public static void main(String[] args) {
        SpringApplication.run(PongApplication.class, args);
    }

    @Bean
    public CommandLineRunner demoData(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            createUserIfMissing(repo, encoder, "talal@pong.com", "1234");
            createUserIfMissing(repo, encoder, "roman@pong.com", "1234");
            createUserIfMissing(repo, encoder, "mohammed@pong.com", "1234");
        };
    }

    private void createUserIfMissing(
            UserRepository repo, PasswordEncoder encoder, String email, String rawPassword) {
        if (!repo.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(encoder.encode(rawPassword));
            repo.save(user);
            System.out.println("Created seed user: " + email);
        }
    }
}

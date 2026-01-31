package paf_grp_i.pong;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

/**
 * Main Spring Boot application class for the Pong game.
 * <p>
 * This application enables scheduled tasks for the game loop and provides
 * demo user data seeding at startup for development and testing purposes.
 * </p>
 */
@SpringBootApplication
@EnableScheduling
public class PongApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PongApplication.class, args);
    }

    /**
     * Creates a command-line runner that seeds demo user accounts on startup.
     * <p>
     * Three test users are created with encoded passwords if they don't already exist
     * in the database. This is intended for development and testing purposes only.
     * </p>
     *
     * @param repo the user repository for database access
     * @param encoder the password encoder for securely hashing passwords
     * @return a {@link CommandLineRunner} that executes the seeding logic
     */
    @Bean
    public CommandLineRunner demoData(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            createUserIfMissing(repo, encoder, "talal@pong.com", "1234");
            createUserIfMissing(repo, encoder, "roman@pong.com", "1234");
            createUserIfMissing(repo, encoder, "mohammed@pong.com", "1234");
        };
    }

    /**
     * Creates a user account if it doesn't already exist in the database.
     * <p>
     * The password is encoded using BCrypt before storage. This method is idempotent
     * and will skip creation if the email already exists.
     * </p>
     *
     * @param repo the user repository for database access
     * @param encoder the password encoder for hashing the password
     * @param email the email address of the user to create
     * @param rawPassword the plain-text password to encode and store
     */
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

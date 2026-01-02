package paf_grp_i.pong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

@SpringBootTest
@Transactional
public class PongApplicationTests {

    @Autowired private UserRepository repo;

    @Test
    public void testUser() {
        String email = "foo@gmx.de";
        User user = new User();
        user.setEmail(email);
        user.setPassword("foopw");
        user.setFirstName("Foo");
        user.setLastName("Bar");

        repo.save(user);
        User foundUser = repo.findByEmail(email);

        // Verify the user exists and game stats are initialized correctly.
        assertNotNull(foundUser, "User should be found in the database");
        assertEquals(email, foundUser.getEmail());
        assertEquals(0, foundUser.getGamesPlayed(), "New user should have 0 games played");
        assertEquals(0, foundUser.getGamesWon(), "New user should have 0 wins");
    }
}

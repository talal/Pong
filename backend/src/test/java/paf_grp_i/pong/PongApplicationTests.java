package paf_grp_i.pong;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

@SpringBootTest
public class PongApplicationTests {

    @Autowired private UserRepository repo;

    @Test
    public void testCreateUser() {
        String email = "foo@gmx.de";
        String lastName = "Bar";
        User user = new User();
        user.setEmail(email);
        user.setPassword("foopw");
        user.setFirstName("Foo");
        user.setLastName(lastName);

        repo.save(user);
        User foundUser = repo.findByEmail(email);

        // do the last names of the found user and the saved user match?
        assertEquals(foundUser.getLastName(), lastName);
    }
}

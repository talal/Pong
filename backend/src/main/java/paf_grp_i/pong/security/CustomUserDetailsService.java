package paf_grp_i.pong.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

/**
 * Custom implementation of Spring Security's {@link UserDetailsService}.
 * Loads user-specific data for authentication by retrieving user information from the database.
 */
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired private UserRepository userRepo;

    /**
     * Loads a user by their username (email address).
     * This method is called by Spring Security during the authentication process.
     *
     * @param username the email address of the user to load
     * @return a {@link UserDetails} object containing the user's authentication information
     * @throws UsernameNotFoundException if no user is found with the given email
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new CustomUserDetails(user);
    }
}

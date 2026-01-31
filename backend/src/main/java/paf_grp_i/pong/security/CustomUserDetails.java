package paf_grp_i.pong.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import paf_grp_i.pong.model.User;

import java.util.Collection;

/**
 * Custom implementation of Spring Security's {@link UserDetails} interface.
 * Wraps a {@link User} entity to provide authentication and authorization information.
 */
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;
    private User user;

    /**
     * Creates a new CustomUserDetails instance.
     *
     * @param user the user entity to wrap
     */
    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Returns the authorities granted to the user.
     *
     * @return null (no role-based authorization implemented)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    /**
     * Returns the username used to authenticate the user.
     * Uses the user's email address as the username.
     *
     * @return the user's email address
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Returns the password used to authenticate the user.
     *
     * @return the user's hashed password
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }
}

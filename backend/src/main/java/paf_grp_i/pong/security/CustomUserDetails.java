package paf_grp_i.pong.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import paf_grp_i.pong.model.User;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;
    private User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }
}

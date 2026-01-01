package paf_grp_i.pong.security;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    public JwtAuthFilter(
            JwtTokenService jwtTokenService,
            UserDetailsService userDetailsService,
            JwtBlacklistService jwtBlacklistService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null
                && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(7);

            if (jwtBlacklistService.isTokenBlacklisted(token)) {
                throw new IllegalArgumentException("Unauthorized: Token has been revoked.");
            }

            try {
                if (jwtTokenService.isValid(token)) {
                    String username = jwtTokenService.getUsername(token);
                    if (username != null && !username.isBlank()) {
                        UserDetails user = userDetailsService.loadUserByUsername(username);
                        var auth = new UsernamePasswordAuthenticationToken(user, null, null);
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext(); // invalid or expired token
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        return p.startsWith("/api/auth/") // exclude auth endpoint and static resources
                || p.startsWith("/css/")
                || p.startsWith("/js/")
                || p.startsWith("/images/");
    }
}

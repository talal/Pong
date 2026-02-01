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

/**
 * JWT authentication filter that intercepts HTTP requests to validate and process
 * JWT tokens for Spring Security authentication.
 *
 * This filter extracts JWT tokens from the Authorization header, validates them,
 * checks against a blacklist, and sets up the security context for authenticated users.
 * It extends {@link OncePerRequestFilter} to ensure single execution per request.
 *
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    /**
     * Constructs a new JwtAuthFilter with required dependencies.
     *
     * @param jwtTokenService service for JWT token validation and parsing
     * @param userDetailsService service for loading user details by username
     * @param jwtBlacklistService service for checking revoked tokens
     */
    public JwtAuthFilter(
            JwtTokenService jwtTokenService,
            UserDetailsService userDetailsService,
            JwtBlacklistService jwtBlacklistService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    /**
     * Filters incoming requests to authenticate users via JWT tokens.
     *
     * This method extracts the JWT from the Authorization header, validates it,
     * checks if it's blacklisted, and establishes authentication in the security context
     * if valid. Invalid or expired tokens result in clearing the security context.
     *
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to continue request processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Determines if this filter should skip processing for specific request paths.
     *
     * The filter is bypassed for authentication endpoints and static resources
     * (CSS, JavaScript, images) to avoid unnecessary token validation overhead.
     *
     *
     * @param request the HTTP request to evaluate
     * @return {@code true} if the filter should not process this request, {@code false} otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        return p.startsWith("/api/auth/") // exclude auth endpoint and static resources
                || p.startsWith("/css/")
                || p.startsWith("/js/")
                || p.startsWith("/images/");
    }
}

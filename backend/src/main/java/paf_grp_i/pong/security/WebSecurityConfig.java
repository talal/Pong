package paf_grp_i.pong.security;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for a stateless JWT-based authentication system.
 * <p>
 * This configuration disables session-based authentication and CSRF protection,
 * enabling JWT token authentication for all secured endpoints. It includes CORS
 * configuration for Vue.js frontend integration and defines public/protected routes.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    /**
     * Provides a custom user details service for loading user information.
     *
     * @return the {@link UserDetailsService} implementation
     */
    @Bean
    protected UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }

    /**
     * Provides a BCrypt password encoder for secure password hashing.
     *
     * @return the {@link BCryptPasswordEncoder} instance
     */
    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the authentication provider using DAO-based authentication.
     * <p>
     * This provider uses the custom user details service and BCrypt password
     * encoder for validating user credentials.
     * </p>
     *
     * @return the configured {@link AuthenticationProvider}
     */
    @Bean
    protected AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configures CORS settings for the Vue.js frontend.
     * <p>
     * Allows requests from localhost:5173 and localhost:5174 with credentials,
     * supporting GET, POST, PUT, DELETE, and OPTIONS methods. Accepts
     * Authorization, Content-Type, and Accept headers.
     * </p>
     *
     * @return the {@link CorsConfigurationSource} for all application paths
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS to all paths
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /**
     * Provides the authentication manager for processing authentication requests.
     *
     * @param config the Spring Security authentication configuration
     * @return the {@link AuthenticationManager} instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configures the main security filter chain for stateless JWT authentication.
     * <p>
     * This chain disables CSRF, sessions, form login, basic auth, and logout,
     * relying entirely on JWT tokens. It applies the JWT authentication filter
     * before the standard username/password filter and configures route-based
     * authorization rules.
     * </p>
     *
     * @param http the {@link HttpSecurity} to configure
     * @param jwtFilter the JWT authentication filter to apply
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter)
            throws Exception {
        http.cors(withDefaults()) // Use the corsConfigurationSource bean defined above
                .csrf(
                        AbstractHttpConfigurer
                                ::disable) // Disable CSRF (not needed for stateless JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(this::configureAuth)
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // Explicitly disable Form Login and Basic Auth to prevent redirects
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Configures authorization rules for HTTP requests.
     * <p>
     * Defines which endpoints are publicly accessible (authentication, static
     * assets, WebSocket handshakes, preflight requests) and requires authentication
     * for all other requests.
     * </p>
     *
     * @param auth the authorization configuration registry
     */
    private void configureAuth(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
                    auth) {
        auth
                // Public API endpoints (Login/Signup)
                .requestMatchers("/api/auth/login", "/api/auth/process_signup")
                .permitAll()

                // Allow image loading
                .requestMatchers("/api/user/avatar")
                .permitAll()

                // WebSocket endpoint (Handshake)
                .requestMatchers("/websocket/**")
                .permitAll()

                // Allow Preflight requests (OPTIONS)
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .permitAll()

                // Error dispatch
                .requestMatchers("/error")
                .permitAll()

                // Static assets (if you decide to bundle Vue later, keep this)
                .requestMatchers("/app/**", "/favicon.ico")
                .permitAll()

                // Everything else requires authentication
                .anyRequest()
                .authenticated();
    }
}

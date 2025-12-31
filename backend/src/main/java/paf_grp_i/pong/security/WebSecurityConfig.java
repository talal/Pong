package paf_grp_i.pong.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    protected UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }
     
    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
     
    @Bean
    protected AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
         
        return authProvider;
    }
    
    //added for SPA support (Vue.js), especially if no client side proxy is used
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
    	CorsConfiguration cors = new CorsConfiguration();
    	cors.setAllowedOrigins(List.of("http://localhost:5173"));
    	cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    	cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    	cors.setAllowCredentials(true);
    	
    	UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    	source.registerCorsConfiguration("/api/**", cors);
    	return source;
    }


    //added for JWT support
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
      return config.getAuthenticationManager();
    }

	//(1) apiChain for JWT handling: /api/** is stateless and uses JWT
	@Bean @Order(1)
	public SecurityFilterChain apiChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
		http
			.securityMatcher("/api/**")
			.cors(Customizer.withDefaults())	//otherwise, browser will block preflight
			.csrf(csrf -> csrf.disable()) 		//stateless API
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/login", "/api/auth/process_signup").permitAll()
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()			//allow preflight
				.anyRequest().authenticated()
			)
//			.exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
//				res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//				res.setContentType("application/json");
//				res.getWriter().write("{\"error\":\"unauthorized\"}");
//			}))
			.authenticationProvider(authenticationProvider())
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable);
		
		return http.build();
	}

	//(2) webChain for classic form login (stateful, i. e. using sessions)
	@Bean @Order(2)
	public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())	//otherwise, browser will block preflight
			//(keep CSRF protection enabled for Thymeleaf forms)
			.authorizeHttpRequests(auth -> auth
				//freely accessible pages
				.requestMatchers("/", "/login", "/signup", "/process_signup", 
								 "/hello-jwt", "/chat-jwt").permitAll()
				//static files (incl. SPA assets), also freely accessible
				.requestMatchers("/app/**", "/css/**", "/js/**", "/images/**", "/webjars/**",
	                       		 "/favicon.ico", "/chat-jwt.js").permitAll()
			    //WebSocket handshake endpoint
			    .requestMatchers("/websocket/**").permitAll()
			    //deliver media publicly
	            .requestMatchers("/media/**").permitAll()
			    //other pages only within session
				.requestMatchers("/hello", "/chat").authenticated()
				.anyRequest().authenticated()
			)
			.authenticationProvider(authenticationProvider())
//	        .formLogin(l -> l		//use standard Spring Security login form
//          	.usernameParameter("email")
//          	.defaultSuccessUrl("/hello")
//          	.permitAll()
//      	)
			.formLogin(l -> l
				.loginPage("/login").permitAll()
				.defaultSuccessUrl("/", true)		//always redirect to menu
			)
			.logout(l -> l.logoutUrl("/logout")
				.logoutSuccessUrl("/login?logout")	//redirect to login form
				.invalidateHttpSession(true)
				.clearAuthentication(true)
				.deleteCookies("JSESSIONID"));

		return http.build();
	}
}

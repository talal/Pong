package paf_grp_i.pong.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;
import paf_grp_i.pong.security.JwtBlacklistService;
import paf_grp_i.pong.security.JwtTokenService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class JwtAuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public JwtAuthController(
            AuthenticationManager am,
            JwtTokenService jts,
            JwtBlacklistService jbs,
            UserDetailsService uds,
            UserRepository repo,
            PasswordEncoder enc) {

        this.authenticationManager = am;
        this.jwtTokenService = jts;
        this.jwtBlacklistService = jbs;
        this.userDetailsService = uds;
        this.userRepo = repo;
        this.passwordEncoder = enc;
    }

    public record SignupRequest(String email, String password) {}

    public record SignupResponse(String message) {}

    @PostMapping("/process_signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        if (req.email() == null
                || req.password() == null
                || req.email().isBlank()
                || req.password().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "E-Mail address and password required"));
        }
        if (userRepo.existsByEmail(req.email())) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "E-Mail address already assigned to another user"));
        }
        User u = new User();
        u.setEmail(req.email());
        u.setPassword(passwordEncoder.encode(req.password()));
        userRepo.save(u);
        return ResponseEntity.status(201).body(new SignupResponse("User created"));
    }

    // adapt fields to login form (here: email + password)
    public record LoginRequest(String email, String password) {}

    public record TokenResponse(String accessToken, long expiresIn) {}

    public record ErrorResponse(String error) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            Authentication auth =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(req.email(), req.password()));
            UserDetails user = (UserDetails) auth.getPrincipal();
            String token = jwtTokenService.generateToken(user);
            long expiresIn = jwtTokenService.getExpirationSeconds();
            return ResponseEntity.ok(new TokenResponse(token, expiresIn));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body(new ErrorResponse("Bad credentials"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<String> refreshToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // remove "Bearer "
        }

        // get username from old token
        String username = jwtTokenService.getUsername(token);

        // add old token to blacklist
        jwtBlacklistService.blacklistToken(token);

        // get the user details that belong to the given username
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // create a new token with the user details
        String newToken = jwtTokenService.generateToken(userDetails);

        return ResponseEntity.ok(newToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        // extract token from header
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // add token to blacklist
        jwtBlacklistService.blacklistToken(token);

        return ResponseEntity.ok().build();
    }
}

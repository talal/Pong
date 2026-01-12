package paf_grp_i.pong.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;
import paf_grp_i.pong.security.JwtBlacklistService;
import paf_grp_i.pong.security.JwtTokenService;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

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

    public record SignupResponse(String message) {}

    @PostMapping("/process_signup")
    public ResponseEntity<?> signup(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "E-Mail address and password required"));
        }
        if (userRepo.existsByEmail(email)) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "E-Mail address already assigned to another user"));
        }

        User u = new User();
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));

        // Image Validation and Processing
        if (file != null && !file.isEmpty()) {
            try {
                validateImage(file);
                u.setAvatar(file.getBytes());
                u.setAvatarContentType(file.getContentType());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            } catch (IOException e) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Error processing image"));
            }
        }

        userRepo.save(u);
        return ResponseEntity.status(201).body(new SignupResponse("User created"));
    }

    // These image constraints are arbitrary. I got them from GitHub because they seem reasonable
    // enough.
    // Reference:
    // https://docs.github.com/en/account-and-profile/reference/profile-reference#profile-picture-requirements
    private void validateImage(MultipartFile file) throws IOException {
        // Check Size (already handled by server config, but good to be explicit)
        if (file.getSize() > 1024 * 1024) { // 1 MiB
            throw new IllegalArgumentException("Image size must be less than 1 MiB");
        }

        // Check Type
        String type = file.getContentType();
        if (type == null || !List.of("image/png", "image/jpeg").contains(type)) {
            throw new IllegalArgumentException("Only PNG and JPEG files are allowed");
        }

        // Check Dimensions
        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) {
            throw new IllegalArgumentException("Invalid image file");
        }
        if (img.getWidth() > 3000 || img.getHeight() > 3000) {
            throw new IllegalArgumentException("Image dimensions must be smaller than 3000x3000px");
        }
    }

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

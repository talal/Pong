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

/**
 * REST controller for JWT-based authentication operations.
 * <p>
 * Handles user signup with optional avatar upload, login with JWT token generation,
 * token refresh, and logout with token blacklisting. All endpoints are publicly
 * accessible and do not require prior authentication.
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
public class JwtAuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new JwtAuthController with required dependencies.
     *
     * @param am the authentication manager for credential verification
     * @param jts the JWT token service for token generation and parsing
     * @param jbs the JWT blacklist service for token revocation
     * @param uds the user details service for loading user information
     * @param repo the user repository for database access
     * @param enc the password encoder for secure password hashing
     */
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

    /**
     * Response record for successful signup operations.
     *
     * @param message confirmation message
     */
    public record SignupResponse(String message) {}

    /**
     * Handles user registration with optional avatar upload.
     * <p>
     * Creates a new user account with the provided email and password. Optionally
     * accepts a profile picture that must meet validation criteria: PNG or JPEG format,
     * under 1 MiB, and dimensions smaller than 3000x3000px. Passwords are hashed
     * using BCrypt before storage.
     * </p>
     *
     * @param email the user's email address (used as username)
     * @param password the user's plain-text password
     * @param file optional profile picture file
     * @return 201 Created with success message, 400 Bad Request for validation errors,
     *         or 409 Conflict if email already exists
     */
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

    /**
     * Validates an uploaded profile picture against size, format, and dimension constraints.
     * <p>
     * Validation rules follow GitHub's profile picture requirements:
     * file size under 1 MiB, PNG or JPEG format only, and dimensions smaller than 3000x3000px.
     * </p>
     *
     * @param file the uploaded image file to validate
     * @throws IllegalArgumentException if the image fails validation
     * @throws IOException if the image cannot be read
     * @see <a href="https://docs.github.com/en/account-and-profile/reference/profile-reference#profile-picture-requirements">GitHub Profile Picture Requirements</a>
     */
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

    /**
     * Request record for login operations.
     *
     * @param email the user's email address
     * @param password the user's password
     */
    public record LoginRequest(String email, String password) {}

    /**
     * Response record for successful authentication.
     *
     * @param accessToken the JWT access token
     * @param expiresIn token lifetime in seconds
     */
    public record TokenResponse(String accessToken, long expiresIn) {}

    /**
     * Response record for authentication errors.
     *
     * @param error the error message
     */
    public record ErrorResponse(String error) {}

    /**
     * Handles user login and JWT token generation.
     * <p>
     * Authenticates the user with provided credentials and returns a JWT access token
     * upon successful authentication. The token can be used for subsequent authenticated
     * requests via the Authorization header.
     * </p>
     *
     * @param req the login credentials
     * @return 200 OK with JWT token and expiration time, or 401 Unauthorized for invalid credentials
     */
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

    /**
     * Refreshes an existing JWT token before expiration.
     * <p>
     * Blacklists the old token and generates a new one with extended validity.
     * The Authorization header must contain the current valid token in Bearer format.
     * </p>
     *
     * @param token the current JWT token from the Authorization header
     * @return 200 OK with new JWT token
     */
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

    /**
     * Handles user logout by invalidating the JWT token.
     * <p>
     * Adds the provided token to the blacklist, preventing its further use for authentication.
     * The Authorization header must contain the token in Bearer format.
     * </p>
     *
     * @param token the JWT token from the Authorization header
     * @return 200 OK with empty response
     */
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

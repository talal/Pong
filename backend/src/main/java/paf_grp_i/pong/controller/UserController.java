package paf_grp_i.pong.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import paf_grp_i.pong.model.User;
import paf_grp_i.pong.repository.UserRepository;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * REST controller for user account management operations.
 * <p>
 * Provides endpoints for authenticated users to change their password,
 * upload or update their profile avatar, and retrieve avatar images.
 * All operations require prior authentication via JWT token.
 * </p>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new UserController with required dependencies.
     *
     * @param userRepo the user repository for database access
     * @param passwordEncoder the password encoder for secure password hashing
     */
    public UserController(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Request record for password change operations.
     *
     * @param oldPassword the user's current password for verification
     * @param newPassword the desired new password
     */
    public record ChangePasswordRequest(String oldPassword, String newPassword) {}

    /**
     * Handles password change requests for authenticated users.
     * <p>
     * Verifies the old password, validates the new password, and updates the
     * user's password with BCrypt hashing. The old password must match the
     * current password, and the new password cannot be empty.
     * </p>
     *
     * @param req the password change request containing old and new passwords
     * @param auth the authentication object containing the user's email
     * @return 200 OK with success message, 400 Bad Request for validation errors,
     *         or 404 Not Found if user doesn't exist
     */
    @PostMapping("/change_password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest req, Authentication auth) {
        String email = auth.getName();
        User user = userRepo.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Old password does not match"));
        }

        if (req.newPassword() == null || req.newPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "New password cannot be empty"));
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepo.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    /**
     * Handles avatar upload or update for authenticated users.
     * <p>
     * Accepts a profile picture that must meet validation criteria: PNG or JPEG format,
     * under 1 MiB, and dimensions smaller than 3000x3000px. Replaces any existing avatar.
     * </p>
     *
     * @param file the uploaded image file
     * @param auth the authentication object containing the user's email
     * @return 200 OK with success message, 400 Bad Request for validation errors,
     *         404 Not Found if user doesn't exist, or 500 Internal Server Error for processing failures
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("file") MultipartFile file, Authentication auth) {
        String email = auth.getName();
        User user = userRepo.findByEmail(email);

        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));

        try {
            validateImage(file);
            user.setAvatar(file.getBytes());
            user.setAvatarContentType(file.getContentType());
            userRepo.save(user);
            return ResponseEntity.ok(Map.of("message", "Avatar updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error processing image"));
        }
    }

    /**
     * Retrieves a user's avatar image.
     * <p>
     * If an email parameter is provided, returns that user's avatar (useful for viewing
     * other players' profiles). Otherwise, returns the authenticated user's own avatar.
     * The response includes the appropriate content type (PNG or JPEG).
     * </p>
     *
     * @param email optional email of the user whose avatar to retrieve
     * @param auth the authentication object containing the requesting user's email
     * @return 200 OK with image data and content type, or 404 Not Found if user doesn't
     *         exist or has no avatar
     */
    @GetMapping("/avatar")
    public ResponseEntity<byte[]> getAvatar(
            @RequestParam(value = "email", required = false) String email, Authentication auth) {
        String targetEmail = (email != null && !email.isBlank()) ? email : auth.getName();
        User user = userRepo.findByEmail(targetEmail);

        if (user == null || user.getAvatar() == null) {
            // Return 404 or a default placeholder via frontend logic
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(user.getAvatarContentType()))
                .body(user.getAvatar());
    }

    /**
     * Validates an uploaded profile picture against size, format, and dimension constraints.
     * <p>
     * Validation rules: file size under 1 MiB, PNG or JPEG format only, and dimensions
     * smaller than 3000x3000px. This mirrors the validation used during signup.
     * </p>
     *
     * @param file the uploaded image file to validate
     * @throws IllegalArgumentException if the image fails validation
     * @throws IOException if the image cannot be read
     */
    private void validateImage(MultipartFile file) throws IOException {
        if (file.getSize() > 1024 * 1024) {
            throw new IllegalArgumentException("Image size must be less than 1 MB");
        }
        String type = file.getContentType();
        if (type == null || !List.of("image/png", "image/jpeg").contains(type)) {
            throw new IllegalArgumentException("Only PNG and JPEG files are allowed");
        }
        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) throw new IllegalArgumentException("Invalid image file");
        if (img.getWidth() > 3000 || img.getHeight() > 3000) {
            throw new IllegalArgumentException("Image dimensions must be smaller than 3000x3000px");
        }
    }
}

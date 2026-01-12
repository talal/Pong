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

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // Change Password
    public record ChangePasswordRequest(String oldPassword, String newPassword) {}

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

    // Upload/Update Avatar
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

    // Serve Avatar
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

    // Reuse validation logic from JWTAuthController.
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

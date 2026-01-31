package paf_grp_i.pong.model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a user in the Pong application.
 * Contains user credentials, profile information, game statistics, and avatar data.
 */
@Getter
@Setter
@Entity
@Table(name = "user")
public class User {

    /** Unique identifier for the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User's email address, used for login. Must be unique. */
    @Column(nullable = false, unique = true, length = 45)
    private String email;

    /** User's hashed password. */
    @Column(nullable = false, length = 64)
    private String password;

    /** User's first name. Optional field. */
    @Column(name = "first_name", nullable = true, length = 20)
    private String firstName;

    /** User's last name. Optional field. */
    @Column(name = "last_name", nullable = true, length = 20)
    private String lastName;

    /** Total number of games won by the user. */
    @Column(nullable = false)
    private int gamesWon = 0;

    /** Total number of games played by the user. */
    @Column(nullable = false)
    private int gamesPlayed = 0;

    /** Binary data of the user's avatar image. Maximum size: 1 MiB. */
    @Lob
    @Column(length = 1048576) // 1 MiB max size
    private byte[] avatar;

    /** MIME type of the avatar image (e.g., "image/png", "image/jpeg"). */
    private String avatarContentType;
}

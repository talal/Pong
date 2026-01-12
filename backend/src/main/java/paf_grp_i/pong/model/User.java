package paf_grp_i.pong.model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 45)
    private String email;

    @Column(nullable = false, length = 64)
    private String password;

    @Column(name = "first_name", nullable = true, length = 20)
    private String firstName;

    @Column(name = "last_name", nullable = true, length = 20)
    private String lastName;

    @Column(nullable = false)
    private int gamesWon = 0;

    @Column(nullable = false)
    private int gamesPlayed = 0;

    // LOB (Large Object) for storing binary image data
    @Lob
    @Column(length = 1048576) // 1 MiB max size
    private byte[] avatar;

    private String avatarContentType;
}
